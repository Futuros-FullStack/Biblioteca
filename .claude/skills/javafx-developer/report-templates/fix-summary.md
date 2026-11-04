# JavaFX 开发者 — 修复总结报告（Fix Summary Report）

> 本 Markdown 模板对应 `report-schema.json` 中的 `fix_summary` 模式。
> 当开发者以修复消费模式（步骤 5.5）运行时使用本模板，
> 消费来自 code-reviewer、runner 或 tester 的修复交接。

## 生成元数据（Generation Metadata）

| 字段 | 值 |
|-------|-------|
| 项目 | {{project}} |
| 开发者版本 | {{developer_version}} |
| 创建时间 | {{created_at}} |
| 模式 | 修复消费 |
| 轮次 | {{round}} |
| 消费来源 | {{sources}}（例如 reviewer、runner、tester） |

## 修复总结（Fix Summary）

**收到的修复总数**：{{total_fixes}}
**已应用**：{{applied_count}}
**已跳过**：{{skipped_count}}
**已回滚**：{{rolled_back_count}}
**已合并（去重）**：{{merged_count}}

### 已应用的修复（Applied Fixes）

| # | 来源 | 问题 ID | 严重程度 | 目标文件 | 目标行 | 修复类型 | 状态 |
|---|--------|----------|----------|-------------|--------------|----------|--------|
{{#each fixes}}
| {{@index}} | {{source}} | {{issue_id}} | {{severity}} | {{target_file}} | {{target_lines}} | {{fix_type}} | {{status}} |
{{/each}}

### 已跳过的修复（Skipped Fixes）

| # | 问题 ID | 目标文件 | 原因 |
|---|----------|-------------|--------|
{{#each skipped_fixes}}
| {{@index}} | {{issue_id}} | {{target_file}} | {{skip_reason}} |
{{/each}}

### 已回滚的修复（Rolled Back Fixes）

| # | 问题 ID | 目标文件 | 回滚原因 |
|---|----------|-------------|-----------------|
{{#each rolled_back_fixes}}
| {{@index}} | {{issue_id}} | {{target_file}} | {{rollback_reason}} |
{{/each}}

## 位置匹配结果（Location Matching Results）

| 修复 ID | 匹配级别 | 是否匹配 |
|--------|-------------|---------|
{{#each location_results}}
| {{fix_id}} | {{match_level}}（1=指纹，2=锚点，3=内容，4=AST） | {{matched}} |
{{/each}}

## 交叉影响警告（Cross-Impact Warnings）

{{#if cross_impact_warnings}}
{{#each cross_impact_warnings}}
- **{{warning_type}}**：{{description}}（受影响文件：{{affected_files}}）
{{/each}}
{{else}}
未检测到交叉影响警告。
{{/if}}

## 回滚事件（Rollback Events）

{{#if rollback_events}}
{{#each rollback_events}}
- **时间戳**：{{timestamp}}
  - **触发原因**：{{trigger}}（例如编译失败）
  - **已恢复文件**：{{files_restored}}
  - **备份位置**：{{backup_location}}
{{/each}}
{{else}}
本轮修复期间未发生回滚事件。
{{/if}}

## 编译验证（Compilation Verification）

| 检查项 | 结果 |
|-------|--------|
| 修复前编译 | {{pre_fix_compile}} |
| 修复后编译 | {{post_fix_compile}} |
| 是否使用增量编译 | {{incremental_compile}} |
| 编译耗时（毫秒） | {{compile_duration_ms}} |

## 结论（Conclusion）

**总体**：{{conclusion}}

{{conclusion_detail}}

---

> **注意**：本修复总结按 `report-schema.json` 序列化为 `developer-report.json`。
> 编排器读取 `fix_summary.fixes[].status` 以决定循环是否继续：
> - 全部 `applied` 且修复后编译通过 → 进入下一门禁
> - 存在 `rolled_back` → 进入回滚恢复（参见编排器状态机）
> - 存在 `skipped` 且 `skip_reason: "not_found"` → 记录警告，可能表示代码发生漂移
