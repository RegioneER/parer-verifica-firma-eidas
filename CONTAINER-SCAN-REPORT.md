## Container scan evidence : gl-container-scanning-report.json
<strong>Run date:</strong> Wed Sep 13 10:51:26 CEST 2023
<br/><strong>Produced by:</strong> <a href="https://gitlab.ente.regione.emr.it/parer/okd/verifica-firma-eidas/-/jobs/122042">Job</a>
| CVE | Message | Description | Severity | Solution | 
|:---:|:---|:---|:---:|:---|
|CVE-2023-32360|Information leak through Cups-Get-Document operation|An authentication issue was addressed with improved state management. This issue is fixed in macOS Big Sur 11.7.7, macOS Monterey 12.6.6, macOS Ventura 13.4. An unauthenticated user may be able to access recently printed documents.|High|Upgrade cups-libs to 1:2.2.6-51.el8_8.1|
|CVE-2022-40433|segmentation fault in ciMethodBlocks|An issue was discovered in function ciMethodBlocks::make_block_at in Oracle JDK (HotSpot VM) 11, 17 and OpenJDK (HotSpot VM) 8, 11, 17, allows attackers to cause a denial of service.|High|No solution provided|
|CVE-2022-40433|segmentation fault in ciMethodBlocks|An issue was discovered in function ciMethodBlocks::make_block_at in Oracle JDK (HotSpot VM) 11, 17 and OpenJDK (HotSpot VM) 8, 11, 17, allows attackers to cause a denial of service.|High|No solution provided|
|CVE-2022-40433|segmentation fault in ciMethodBlocks|An issue was discovered in function ciMethodBlocks::make_block_at in Oracle JDK (HotSpot VM) 11, 17 and OpenJDK (HotSpot VM) 8, 11, 17, allows attackers to cause a denial of service.|High|No solution provided|
|CVE-2023-24329|urllib.parse url blocklisting bypass|An issue in the urllib.parse component of Python before 3.11.4 allows attackers to bypass blocklisting methods by supplying a URL that starts with blank characters.|High|Upgrade platform-python to 3.6.8-51.el8_8.1|
|CVE-2023-24329|urllib.parse url blocklisting bypass|An issue in the urllib.parse component of Python before 3.11.4 allows attackers to bypass blocklisting methods by supplying a URL that starts with blank characters.|High|Upgrade python3-libs to 3.6.8-51.el8_8.1|
|CVE-2022-45868|The web-based admin console in H2 Database Engine through 2.1.214 can ...|** DISPUTED CHANGELOG.md CONTAINER-SCAN-REPORT.md Dockerfile INSTALL.md OKD.md README.md RELEASE-NOTES.md docker_build gl-container-scanning-report.json gl-container-scanning-report.txt pom.xml profiles src tmp.md The web-based admin console in H2 Database Engine through 2.1.214 can be started via the CLI with the argument -webAdminPassword, which allows the user to specify the password in cleartext for the web admin console. Consequently, a local user (or an attacker that has obtained local access through some means) would be able to discover the password by listing processes and their arguments. NOTE: the vendor states "This is not a vulnerability of H2 Console ... Passwords should never be passed on the command line and every qualified DBA or system administrator is expected to know that."|High|Upgrade com.h2database:h2 to 2.2.220|
|CVE-2022-44729|Server-Side Request Forgery vulnerability|Server-Side Request Forgery (SSRF) vulnerability in Apache Software Foundation Apache XML Graphics Batik.This issue affects Apache XML Graphics Batik: 1.16.

On version 1.16, a malicious SVG could trigger loading external resources by default, causing resource consumption or in some cases even information disclosure. Users are recommended to upgrade to version 1.17 or later.

|High|Upgrade org.apache.xmlgraphics:batik-bridge to 1.17|
|CVE-2022-44729|Server-Side Request Forgery vulnerability|Server-Side Request Forgery (SSRF) vulnerability in Apache Software Foundation Apache XML Graphics Batik.This issue affects Apache XML Graphics Batik: 1.16.

On version 1.16, a malicious SVG could trigger loading external resources by default, causing resource consumption or in some cases even information disclosure. Users are recommended to upgrade to version 1.17 or later.

|High|Upgrade org.apache.xmlgraphics:batik-transcoder to 1.17|
