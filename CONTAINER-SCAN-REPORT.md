## Container scan evidence CVE
<strong>Image name:</strong> registry.ente.regione.emr.it/parer/okd/verifica-firma-eidas:sast
<br/><strong>Run date:</strong> Wed May 22 15:30:46 CEST 2024
<br/><strong>Produced by:</strong> <a href="https://gitlab.ente.regione.emr.it/parer/okd/verifica-firma-eidas/-/jobs/243866">Job</a>
<br/><strong>CVE founded:</strong> 3
| CVE | Description | Severity | Solution | 
|:---:|:---|:---:|:---|
| [CVE-2024-2961](http://www.openwall.com/lists/oss-security/2024/04/17/9)|The iconv() function in the GNU C Library versions 2.39 and older may overflow the output buffer passed to it by up to 4 bytes when converting strings to the ISO-2022-CN-EXT character set, which may be used to crash an application or overwrite a neighbouring variable.|High|Upgrade glibc to 2.28-236.el8_9.13|
| [CVE-2024-2961](http://www.openwall.com/lists/oss-security/2024/04/17/9)|The iconv() function in the GNU C Library versions 2.39 and older may overflow the output buffer passed to it by up to 4 bytes when converting strings to the ISO-2022-CN-EXT character set, which may be used to crash an application or overwrite a neighbouring variable.|High|Upgrade glibc-common to 2.28-236.el8_9.13|
| [CVE-2024-2961](http://www.openwall.com/lists/oss-security/2024/04/17/9)|The iconv() function in the GNU C Library versions 2.39 and older may overflow the output buffer passed to it by up to 4 bytes when converting strings to the ISO-2022-CN-EXT character set, which may be used to crash an application or overwrite a neighbouring variable.|High|Upgrade glibc-minimal-langpack to 2.28-236.el8_9.13|
