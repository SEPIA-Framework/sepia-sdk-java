# sepia-sdk-java
A set of classes to develop services for SEPIA in Java

### Credentials
Create an account on the SEPIA server you want to develop for.
Ask the admin to add the 'developer' role to the account (see [Wiki](https://github.com/SEPIA-Framework/sepia-docs/wiki/Create-and-Edit-Users) for help).

### Service upload interface
The SEPIA-Assist server has an endpoint for service uploads, e.g.: http://localhost:20721/upload-service  
To use the interface make sure the server has SDK support enabled (via admin-tools or enable_sdk=true in assist.*.properties).

### Quickstart
- Import the maven project into the IDE of your choice (tested with Eclipse).
- Create a package for your developer account ID under 'net.b07z.sepia.sdk.services' (e.g. "uid1010" -> net.b07z.sepia.sdk.services.uid1010).
- Open 'Settings/sdk.properties' and put in your credentials (ID + password of your SEPIA account with 'developer' role) and the endpoint URL of your SEPIA-Assist server.
- Modify and run the various '[.main.Test***.java](https://github.com/SEPIA-Framework/sepia-sdk-java/tree/dev/src/main/java/net/b07z/sepia/sdk/main)' classes to test the upload of one of the demo services. Check the result for errors. Make sure you installed a jdk on the S.E.P.I.A. server (e.g.: sudo apt install openjdk-8-jdk)
- Check-out the examples under '[.services.uid1007.*](https://github.com/SEPIA-Framework/sepia-sdk-java/tree/dev/src/main/java/net/b07z/sepia/sdk/services/uid1007)' to get an idea of how a SDK custom service works (until a real documentation is ready ^^).
- Open your SEPIA client, login with the same user ID you used for development and make a real test of your custom service.

### Good to know
Services that have been uploaded are only available for the user that uploaded them unless you upload them with the 'assistant' user (core-account).
