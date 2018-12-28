# sepia-sdk-java
A set of classes to develop services for SEPIA in Java

### Credentials
Create an account on the SEPIA server you want to develop for.
Ask the admin to add the 'developer' role to the account (see [Wiki](https://github.com/SEPIA-Framework/sepia-docs/wiki/Create-and-Edit-Users) for help).

### Service upload interface
SEPIA-Assist server, e.g.: http://localhost:20721/upload-service

### Quickstart
- Import the maven project into the IDE of your choice (tested with Eclipse).
- Create a package for your developer account ID under 'net.b07z.sepia.sdk.services' (e.g. "uid1007" -> net.b07z.sepia.sdk.services.uid1007).
- Open 'Settings/sdk.properties' and put in your credentials (ID + password of your SEPIA account with 'developer' role) and the endpoint URL of your SEPIA-Assist server.
- Modify and run 'TestViaUpload.java' to test the upload of one of the demo classes. Check the result for errors.
- Open a SEPIA client, login with the same user ID you used for development and make a real test of your new custom service.

### Good to know
Services that have been uploaded are only available for the user that uploaded them unless you upload them with the 'assistant' user (core-account).
