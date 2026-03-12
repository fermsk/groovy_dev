
def getColumnValue(def cn) {
    def columnValues = [
        'processname':       0x75,
        'pid':               0x76,
        'result':            0x78,
        'detail':            0x79,
        'duration':          0x8D,
        'imagepath':         0x84,
        'relativetime':      0x8C,
        'command line':      0x82,
        'user':              0x83,
        'operation':         0x77,
        'session':           0x85,
        'path':              0x87,
        'tid':               0x88,
        'timeofday':         0x8E,
        'version':           0x91,
        'eventclass':        0x92,
        'authentication id': 0x93,
        'virtualized':       0x94,
        'integrity':         0x95,
        'category':          0x96,
        'parent pid':        0x97,
        'architecture':      0x98,
        'sequence':          0x7A,
        'company':           0x80,
        'description':       0x81
    ]
    cv = columnValues[cn.toLowerCase()]
    assert cv != null : "Procmon filter. Unknown Column: ${cn}"
    return cv
}

def getRelation(def rn) {
    def relations = [
        'is':          0x00,
        'is not':      0x01,
        'less than':   0x02,
        'more than':   0x03,
        'begins with': 0x04,
        'ends with':   0x05,
        'contains':    0x06,
        'excludes':    0x07
    ]
    rv = relations[rn.toLowerCase()]
    assert rv != null : "Procmon filter. Unknown Relation: ${rn}"
    return rv
}

def getAction(def an) {
    def actions = [
        'exclude': 0x00,
        'include': 0x01
    ]
    av = actions[an.toLowerCase()]
    assert av != null : "Procmon filter. Unknown Action: ${an}"
    return av
}

def hex2bytes16(String hex) {
    def v = hex.toLowerCase()
    def first, second
    if (v.length() > 2) {
        first  = v.substring(1, Math.min(3, v.length()))
        second = v.substring(0, 1)
    } else {
        first  = v
        second = "0"
    }
    def b1 = Integer.parseInt(first,16)
    def b2 = Integer.parseInt(second,16)
    return [b1, b2]
}

def toHex2(def n) {
    return Integer.toHexString(n).toLowerCase()
}

def charToAsciiBytes(char c) {
    int code = (int) c
    def hex = Integer.toHexString(code)
    return hex2bytes16(hex)
}

def buildFilterRules(def filters) {

    def bytes = []
    bytes << 0x01
    
    def String numFiltersHex = Integer.toHexString(filters.size())
    bytes.addAll(hex2bytes16(numFiltersHex))
    
    bytes << 0x00 << 0x00
    filters.each { f ->

        def cv = getColumnValue(f[0])
        bytes << cv << 0x9C
        bytes << 0x00 << 0x00

        def rv = getRelation(f[1])
        bytes << rv

        bytes << 0x00 << 0x00 << 0x00
        
        def av = getAction(f[3])
        bytes << av
        
        def vl = ((f[2]).length() * 2) + 2
        bytes.addAll(hex2bytes16(toHex2(vl)))
        
        bytes << 0x00 << 0x00
        
        (f[2]).toCharArray().each { ch ->
            bytes.addAll(charToAsciiBytes(ch as char))
        }
        
        bytes << 0x00 << 0x00

        if ((f[0]).toLowerCase() == 'pid' || (f[0]).toLowerCase() == 'parent pid') {
            def pid = Integer.parseInt(f[2])
            def quotient = (int) Math.floor(pid / 256)
            def remainder = pid % 256
            bytes << remainder << quotient
        } else {
            bytes << 0x00 << 0x00
        }
        
        bytes << 0x00 << 0x00 << 0x00 << 0x00 << 0x00 << 0x00
        
    }
    
    return bytes
}

def setFilterRules(def data, def nodeName = 'windows-docker') {
    def regPath = 'HKCU:\\Software\\Sysinternals\\Process Monitor'
    def valueName = 'FilterRules'
    def hexList = data.collect { b -> String.format("0x%02x", (b & 0xFF)) }.join(',')

    def powershellScript = "\$bytes = [Byte[]](${hexList}); " +
                "New-Item -Path '${regPath}' -Force | Out-Null; " +
                "New-ItemProperty -Path '${regPath}' -Name '${valueName}' -PropertyType Binary -Value \$bytes -Force | Out-Null"

    node(nodeName) {
        powershell powershellScript
    }
}

def getParentPID() {
    def powershellScript = '''
        $PPID = $PID
        Write-Host $PPID
        do {
            $ParentProcess = Get-WmiObject Win32_Process -Filter "ProcessId = $PPID"
            if ( $ParentProcess.CommandLine -eq 'C:\\Windows\\system32\\cexecsvc.exe' ) {
                exit $PPID
            }
            $PPID = (Get-WmiObject Win32_Process -Filter "ProcessId = $PPID").ParentProcessId
            Write-Host $PPID
        } until ( -not $PPID )
        exit 0
    '''
    def pid = powershell ( script: powershellScript, returnStdout: true, returnStatus: true )
    assert pid != 0 : "Procmon. Parent PID could not be determined"
    return pid
}

def isProcmon() {
    def pm = ''
    try {
        pm = bat(script: 'tasklist | findstr /R "Procmon[6]*[4]*[a]*.exe"', returnStdout: true).trim()
        return true
    } catch(e) {
        return false
    }
}

def waitingProcmon() {
    while ( isProcmon() ) {
        println ('Process Monitor is running. Waiting...')
        sleep(60)
    }
}

def startProcmon(def nodeName, def fileName) {
    node(nodeName) {
        waitingProcmon()
        bat 'schtasks /Create /TN "Process Monitor" /TR "\"c:\\exe\\procmon64.exe\" /accepteula /minimized /quiet /backingfile ' + fileName + '" /SC ONCE /ST 00:00 /F'
        bat 'schtasks /Run /TN "Process Monitor"'
    }
}

def terminateProcmon(def nodeName) {
    node(nodeName) {
        if ( isProcmon() ) {
            bat "start /wait c:\\exe\\procmon64.exe /accepteula /minimized /quiet /terminate"
            bat 'schtasks /Delete /TN "Process Monitor" /F'
        }
    }
}

def toCsv(def nodeName, def pmlFile, def csvFile, def compress = true) {
    node(nodeName) {
        waitingProcmon()
        bat 'start /wait c:\\exe\\procmon64.exe /accepteula /minimized /quiet /openlog ' + pmlFile + ' /saveas ' + csvFile
        if ( compress ) {
            bat 'C:\\exe\\7z\\7za.exe a -mmt8 -mx8 -sdel ' + csvFile + '.7z ' + csvFile
        }
    }
}
