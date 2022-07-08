<%@page import="java.io.FileNotFoundException"%>
<%@page import="java.io.IOException"%>
<%@page import="java.io.PrintWriter"%>
<!DOCTYPE html>
<%@page import="java.util.jar.Attributes"%>
<%@page import="java.io.InputStream"%>
<%@page import="java.util.jar.Manifest"%>
<html>
    <head>
        <title>Servizi REST e SOAP verifica firma EIDAS</title>
    </head>

    <body>
        <h1>Servizi web di validazione firme</h1>
        <%
            String name = "undefined";
            String version = "undefined";
            try {
                ServletContext webApp = getServletConfig().getServletContext();

                InputStream inputStream = webApp.getResourceAsStream("/META-INF/MANIFEST.MF");
                Manifest manifest = new Manifest(inputStream);

                Attributes attributes = manifest.getMainAttributes();

                name = attributes.getValue("App-Name");
                version = attributes.getValue("App-Version");
            } catch (Exception e) {
                out.println("<!-- Eccezione: ");
                try (PrintWriter pw = new PrintWriter(out);) {
                    e.printStackTrace(pw);
                }
                out.println("-->");
            }
        %>


        <p>
            Questa applicazione serve ad esporre i ws relativi alla verifica delle
            firme EIDAS. 
        </p>

        <footer>
            <p>
                Nome applicazione: <strong><%=name%></strong>
            </p>
            <p>
                Versione: <strong><%=version%></strong>
            </p>
        </footer>
    </body>

</html>