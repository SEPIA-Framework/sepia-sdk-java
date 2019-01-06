package net.b07z.sepia.sdk.main;

import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.b07z.sepia.sdk.connect.DeleteService;
import net.b07z.sepia.sdk.services.uid1007.HelloWorld;
import net.b07z.sepia.sdk.services.uid1007.SandboxTest;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.tools.JSON;

public class TestDeleteServices {

	private static final Logger log = LoggerFactory.getLogger(TestDeleteServices.class);
			
	/**
	 * Test deletion of custom services from server and user account. 
	 */
	public static void main(String[] args) {
		
		//Data for endpoint
		JSONArray commands = new JSONArray();
		JSONArray services = new JSONArray();
		
		//Services to remove:
        ServiceInterface service1 = new HelloWorld();
        JSON.add(commands, service1.getInfo("").intendedCommand);
        JSON.add(services, service1.getClass().getSimpleName());
        
        ServiceInterface service2 = new SandboxTest();
        JSON.add(commands, SandboxTest.getCmdName());
        JSON.add(services, service2.getClass().getSimpleName());
        
        log.info("Delete services connected to following commands: " + commands.toJSONString());
        log.info("Delete services with following names: " + services.toJSONString());
        log.info("Service deletion success? " + DeleteService.deleteServicesConnectedToCommands(commands, services));
	}

}
