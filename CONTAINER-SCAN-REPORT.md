## Container scan evidence CVE
<strong>Image name:</strong> registry.ente.regione.emr.it/parer/okd/verifica-firma-eidas:sast
<br/><strong>Run date:</strong> Wed May 7 13:00:29 CEST 2025
<br/><strong>Produced by:</strong> <a href="https://gitlab.ente.regione.emr.it/parer/okd/verifica-firma-eidas/-/jobs/612321">Job</a>
<br/><strong>CVE founded:</strong> 1
| CVE | Description | Severity | Solution | 
|:---:|:---|:---:|:---|
| [CVE-2025-27363](http://www.openwall.com/lists/oss-security/2025/03/13/1)|An out of bounds write exists in FreeType versions 2.13.0 and below (newer versions of FreeType are not vulnerable) when attempting to parse font subglyph structures related to TrueType GX and variable font files. The vulnerable code assigns a signed short value to an unsigned long and then adds a static value causing it to wrap around and allocate too small of a heap buffer. The code then writes up to 6 signed long integers out of bounds relative to this buffer. This may result in arbitrary code execution. This vulnerability may have been exploited in the wild.|High|No solution provided|
