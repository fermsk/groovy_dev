# Библиотека реализует следующие функции
Для импорта библиотеки необходимо в начало скрипта добавить строки
@Library('ru.aladdin.jenkins.bhl')
import ru.aladdin.jenkins.bhl.AssemblyEnvironmentDocker

## Модуль addons предоставляет следующие функции:
### getGitlabSHA1Jenkinsfile(script)
Функция получает коммит репозитория, из которого взят Jenkinsfile.
Параметр script является указателем на скрипт из которого вызывается функция.
Функция 
Пример вызова из Jenkins pipeline скрипта осуществляется следующим образом:
def sha1 = addons.getGitlabSHA1Jenkinsfile(this)

### getStrLabel( l )
Возвращает строковое представление label'а l, который в свою очередь
представляет собой ассоциативный массив следующего вида
[ os: '<os>', arch: '<arch>', debug: true | false ]
Пример вызова из Jenkins pipeline скрипта осуществляется следующим образом:
def strLabel = addons.getStrLabel( [ os: 'windows', arch: 'all', debug: true ] )
В данном примере переменная strLabel будет иметь значение "windows-debug-all"

### addPromotionAlpha(script, addpds = [:] )
Добавляет promotion для сборочного задания
Параметр script является указателем на скрипт из которого вызывается функция.
Через параметр addpds можно передать дополнительный опции для настройки promotion:

- notCopyToSMBShare  - принимает значение 'true' или 'flase', не копировать артефакты
                       на smb share (\\aladdin.ru\main\R&D\Artifacts\Test\)
- artifactRepo       - Git репозиторий для выкладывания артефактов сборки
- artifactSMBPath    - Путь к папке относительно \\ALADDIN.RU\main\S_RnD\Artifacts\Test, в которую будут выкладываться артефакты сборки

Пример использования:
addons.addPromotionAlpha(this, [ notCopyToSMBShare: true, artifactRepo: 'http://gitlab.aladdin.ru/artifacts/vendors/openssl', artifactSMBPath: 'vendors/openssl' ] )

### getWorkspaceURL()

### saveDockerContainer( script, projectName, branch, version, cloud, containerId, credentialsId = '879c6da5-0a0b-4a83-9044-09a124747a06' )

### saveBuildLog(script, buildUrl, credentialsId = '879c6da5-0a0b-4a83-9044-09a124747a06')

### collectSourcesChecksumsUfix(reportName) 
Собирает контрольные суммы утилитой ufix (linux) В качестве аргумента принимает имя отчета, вызывать нужно сразу после добавления исходников в проект.

### collectBinaryChecksumsUfix(reportName, binDir, binExtensions)
Собирает контрольные суммы с артефактов. В качестве аргументов передаются следующие значения:
- reportName(1) - имя отчета. Строка
- binDir(2) - директория с файлами, в которой необходимо выполнить поиск. Строка
- binExtensions(3) - Расширения файлов, с которых необходимо снять КС. Список (массив строк) в формате ["\*.bin","\*.so","*.txt"]