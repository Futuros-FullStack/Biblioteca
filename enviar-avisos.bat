@echo off
rem Genera y envia los avisos de vencimiento. Programar con el Programador de tareas de Windows (una vez al dia).
rem Requiere haber compilado el proyecto (Build en NetBeans o "mvn compile") para que existan target\classes y target\lib.
cd /d "%~dp0"
echo === %date% %time% === >> avisos.log
java -cp "target\classes;target\lib\*" com.mycompany.bibliotecasistema.notificaciones.EnviarAvisos >> avisos.log 2>&1
