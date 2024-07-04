## Container scan evidence CVE
<strong>Image name:</strong> registry.ente.regione.emr.it/parer/okd/verifica-firma-eidas:sast
<br/><strong>Run date:</strong> Wed Apr 10 16:45:14 CEST 2024
<br/><strong>Produced by:</strong> <a href="https://gitlab.ente.regione.emr.it/parer/okd/verifica-firma-eidas/-/jobs/216130">Job</a>
<br/><strong>CVE founded:</strong> 1
| CVE | Description | Severity | Solution | 
|:---:|:---|:---:|:---|
| [CVE-2024-28182](https://access.redhat.com/security/cve/CVE-2024-28182)|A vulnerability was found in how nghttp2 implements the HTTP/2 protocol. There are insufficient limitations placed on the amount of CONTINUATION frames that can be sent within a single stream. This issue could allow an unauthenticated remote attacker to send packets to vulnerable servers, which could use up compute or memory resources to cause a Denial of Service.|High|No solution provided|
