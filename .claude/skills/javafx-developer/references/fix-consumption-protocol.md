# Fix Consumption Protocol — Developer Reference

> 本文件定义 `javafx-developer` 消费 Fix Handoff 的完整协议。
> Fix Handoff 7 字段格式定义见 `_shared/fix-handoff-format.md`。
> severity → fix_priority 映射见 `_shared/severity-classification.md` §2。

---

## 1. Dedup 规则（去重）

### 1.1 `dedup_merged_from` 字段检测（关键分流）

解析报告时，首先检测是否有任意条目已携带 `dedup_merged_from` 字段：

- **存在** → orchestrator 已完成合并去重。**跳过所有去重操作**，直接消费预合并批次，进入 §1.4 重新排序。**不得再次去重**，否则会丢弃 orchestrator 的可追溯性元数据。
- **不存在** → developer 处于 standalone 模式，执行 §1.2 standalone 去重。

### 1.2 Standalone Deduplication

当无任何条目携带 `dedup_merged_from` 时，对**同一 `target_file`** 且 **`target_lines` 范围重叠**的条目进行去重：

1. **保留 severity 更高者**：`Critical > Major > Minor > Info`
2. **若 severity 相同**：保留 `fix_priority` 数字更小者
3. **记录被丢弃条目**：写入保留条目的 `dedup_merged_from` 字段，保持可追溯性

### 1.3 Refactorer 特殊处理

`refactor-handoff.json` 将 7 字段嵌入每个 `refactoring_plan[]` 数组项中（字段语义完全相同，无需转换）。每个 `refactoring_plan[]` 项解析为单个 fix 条目。

### 1.4 Re-Sort

对（去重后或预合并的）列表，按 `fix_priority` 升序（`1` = 最高）跨所有源重新排序，然后按 `target_file` 分组进入 §2 并发分组。

---

## 2. 并发 Fix 分组规则

### 2.1 File-Based Grouping

将所有 fix 条目按 `target_file` 分组。

### 2.2 Parallel Eligibility

- **不同文件**的分组 → 可并行执行（inter-file parallel）
- **同一文件**的分组（经 §3 AST 重定位后）→ 必须串行执行（intra-file serial）

### 2.3 Intra-File Ordering

每个文件组内，fix 按以下规则排序：
- **主排序**：`fix_priority` 升序（`1` = 最高，最先应用）
- **次排序**：`target_lines` 起始行**降序**（最高行号优先 — 防止前序修复改变后续行号）
- 每次修复后，根据行数增量重新计算同组内剩余 fix 的行号

### 2.4 Concurrency Limit

并发文件组数上限为 `min(file_group_count, 4)` — 硬上限 4 个并发文件修改。

### 2.5 Dependency-Aware Scheduling

若 Group A 中的某个 fix 创建了 Group B 中 fix 引用的新文件（通过 refactoring handoff 的 `new_files` 字段检测），则 Group B 必须等待 Group A 完成。构建 DAG，拓扑排序执行。

---

## 3. 4 级 Location Matching 层次

当 `target_lines` 因前序修复发生行号漂移（line drift）时，按以下 4 级层次依次尝试定位正确代码位置。

### Level 1 — Fingerprint-First Matching（primary）

- 计算 `target_lines` 处代码的 SHA-256（归一化：whitespace-trimmed），与 `code_fingerprint` 比较
- **匹配成功**：直接应用修复，状态 `applied`
- **匹配失败**：进入 Level 2

### Level 2 — Anchor-Based Matching（secondary）

- 使用 `anchor_pattern`（前 2 行 + 后 2 行上下文签名）在文件内搜索
- **匹配成功**（恰好一处）：在该位置应用，状态 `applied (relocated by anchor)`
- **匹配失败**（歧义或无匹配）：进入 Level 3

### Level 3 — Content-Based Matching（fallback）

- 在 `target_lines` 指定范围的 **±10 行**内搜索报告中的 "Problematic Code" 文本
- **匹配成功**：在找到的位置应用，状态 `applied (relocated)`
- **匹配失败**：进入 Level 4

### Level 4 — AST Signature Matching（refactor-resistant）

- 使用 `ast_node_signature` 按结构标识定位：
  1. 解析签名，提取完全限定类名与可选成员名（按 `#` 分割）
  2. 在项目源码树中搜索该类文件
  3. 若类文件与 `target_file` **不同** → 更新 `target_file` 为实际位置
  4. 若签名含方法 → 找到匹配方法声明，在方法实际位置应用
  5. 若签名含字段 → 找到字段声明应用
  6. 若只有类签名 → 定位类声明应用
  7. 若类不在预期包中 → 按类名项目级搜索
  8. 若类内找不到成员 → 状态 `skipped (AST node not found)`
- **匹配成功**：状态 `applied (relocated by AST: {old_file} → {new_file})`
- **匹配失败**：状态 `skipped (line drift)`

### No Match（全部失败）

状态 `skipped (line drift)`，在 Fix Summary 中包含详情供人工排查。

### AST Relocation 与 Group Reassignment

若 Level 4 将 fix 重定位到**不同文件**：该 fix 移动到目标文件的分组；若目标文件分组正在执行，该 fix 延迟到 parallel 阶段后串行应用。

---

## 4. Intra-File Batch Ordering

同一文件内多修复按**逆行号顺序**（最高行号优先）应用。每次修复后，根据已应用修复的行数增量重新计算同文件内剩余修复的 `target_lines`。
