## Container scan evidence CVE
<strong>Image name:</strong> registry.ente.regione.emr.it/parer/okd/verifica-firma-eidas:sast
<br/><strong>Run date:</strong> Fri Aug 22 12:26:01 CEST 2025
<br/><strong>Produced by:</strong> <a href="https://gitlab.ente.regione.emr.it/parer/okd/verifica-firma-eidas/-/jobs/724322">Job</a>
<br/><strong>CVE founded:</strong> 3
| CVE | Description | Severity | Solution | 
|:---:|:---|:---:|:---|
| [CVE-2025-5914](https://access.redhat.com/errata/RHSA-2025:14130)|A vulnerability has been identified in the libarchive library, specifically within the archive_read_format_rar_seek_data() function. This flaw involves an integer overflow that can ultimately lead to a double-free condition. Exploiting a double-free vulnerability can result in memory corruption, enabling an attacker to execute arbitrary code or cause a denial-of-service condition.|High|Upgrade libarchive to 3.3.3-6.el8_10|
| [CVE-2025-7425](https://access.redhat.com/errata/RHSA-2025:12447)|A flaw was found in libxslt where the attribute type, atype, flags are modified in a way that corrupts internal memory management. When XSLT functions, such as the key() process, result in tree fragments, this corruption prevents the proper cleanup of ID attributes. As a result, the system may access freed memory, causing crashes or enabling attackers to trigger heap corruption.|High|Upgrade libxml2 to 2.9.7-21.el8_10.2|
| [CVE-2025-6965](https://access.redhat.com/errata/RHSA-2025:11992)|There exists a vulnerability in SQLite versions before 3.50.2 where the number of aggregate terms could exceed the number of columns available. This could lead to a memory corruption issue. We recommend upgrading to version 3.50.2 or above.|High|Upgrade sqlite-libs to 3.26.0-20.el8_10|
