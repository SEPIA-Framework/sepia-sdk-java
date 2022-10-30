# SEPIA Java SDK
A set of tools and classes to develop services for SEPIA in Java.

## Credentials
Create an account on the SEPIA server you want to develop for.
Ask the admin to add the 'developer' role to the account (see [Wiki](https://github.com/SEPIA-Framework/sepia-docs/wiki/Create-and-Edit-Users) for help).

## Service upload interface
The SEPIA-Assist server has an endpoint for service uploads, e.g.: http://localhost:20721/upload-service  
To use the interface make sure the server has SDK support enabled (via [Control-HUB](https://github.com/SEPIA-Framework/sepia-admin-tools/tree/master/admin-web-tools) or enable_sdk=true in assist.*.properties).  

## Service upload via Control-HUB
You can use the 'Code-UI' page of the Control-HUB to edit and upload services created with the SDK. See the [SEPIA extensions](https://github.com/SEPIA-Framework/sepia-extensions) repository for more info.

## Quickstart
- Import the maven project into the IDE of your choice (tested with Eclipse).
- Run `mvn clean` and `mvn install` once to make sure SEPIA libraries are available in Java classpath
- Make sure you've installed a JAVA JDK on the S.E.P.I.A. server (e.g.: `sudo apt-get install -y openjdk-11-jdk-headless`). This is required to compile new classes during runtime.
- Create a package for your developer account ID under 'net.b07z.sepia.sdk.services' (e.g. "uid1010" -> net.b07z.sepia.sdk.services.uid1010).
- Open 'Settings/sdk.properties' and put in your credentials (ID + password of your SEPIA account with 'developer' role) and the endpoint URL of your SEPIA-Assist server.
- Modify and run the various '[.main.Test***.java](https://github.com/SEPIA-Framework/sepia-sdk-java/tree/dev/src/main/java/net/b07z/sepia/sdk/main)' classes to test the upload of one of the demo services. Check the result for errors.
- Check-out the examples under '[.services.uid1007.*](https://github.com/SEPIA-Framework/sepia-sdk-java/tree/dev/src/main/java/net/b07z/sepia/sdk/services/uid1007)' to get an idea of how a SDK custom service works. You can find more examples inside the 'SEPIA Extensions' repository.
- Open your SEPIA client, login with the same user ID you used for development and make a real test of your custom service.

### How to load Javadoc

Because you can't load the Javadoc from Maven Central yet the Javadocs for the packages `net.b07z.sepia.server.core`, `net.b07z.sepia.websockets` and `net.b07z.sepia.server.assist` are included as JAR files in the `libs` folder.  
You can load them in your favorite IDE via the build-path properties. In Eclipse for example do:
- Go to your project `sepia-sdk-java` project in your workspace
- Expand the section "Maven Dependencies" and look for e.g. `sepia-assist-vX.Y.Z.jar`
- Open the properties (right click on .jar -> properties), then open the section "Javadoc location"
- Choose "Javadoc in archive" -> "Workspace file" and add `sepia-sdk-java/libs/sepia-assist-vX.Y.Z-javadoc.jar`
- Click "Apply and close" and repeat the steps for the other SEPIA JAR files

## Good to know
Services that have been uploaded are only available for the user that uploaded them unless you set 'info.makePublic()' in the code and upload them with the **'assistant' user** (core-account, usually uid1005).
