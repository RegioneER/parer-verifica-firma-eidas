## Container scan evidence CVE
<strong>Image name:</strong> registry.ente.regione.emr.it/parer/okd/verifica-firma-eidas:sast
<br/><strong>Run date:</strong> Fri Mar 7 16:01:27 CET 2025
<br/><strong>Produced by:</strong> <a href="https://gitlab.ente.regione.emr.it/parer/okd/verifica-firma-eidas/-/jobs/540796">Job</a>
<br/><strong>CVE founded:</strong> 2
| CVE | Description | Severity | Solution | 
|:---:|:---|:---:|:---|
| [CVE-2024-56171](https://access.redhat.com/security/cve/CVE-2024-56171)|libxml2 before 2.12.10 and 2.13.x before 2.13.6 has a use-after-free in xmlSchemaIDCFillNodeTables and xmlSchemaBubbleIDCNodeTables in xmlschemas.c. To exploit this, a crafted XML document must be validated against an XML schema with certain identity constraints, or a crafted XML schema must be used.|High|No solution provided|
| [CVE-2025-24928](https://access.redhat.com/security/cve/CVE-2025-24928)|libxml2 before 2.12.10 and 2.13.x before 2.13.6 has a stack-based buffer overflow in xmlSnprintfElements in valid.c. To exploit this, DTD validation must occur for an untrusted document or untrusted DTD. NOTE: this is similar to CVE-2017-9047.|High|No solution provided|
