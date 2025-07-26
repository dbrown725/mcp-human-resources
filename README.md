# mcp-human-resources
Spring Boot MCP Server implementation focused on exposing Human Resources department data<br><br>
This was built as a learning tool. Do not use it as a template for a Production application. Giving your LLM access to create, update and delete database rows is probably not a good idea.

## Installation
Assumes Linux with Java, Maven, node and npm installed

1. Clone the repository:

```bash
git clone https://github.com/dbrown725/mcp-human-resources.git
cd mcp-human-resources
```

2. Setup Brave Search: Acquire API Key, update configuration file and install Node module(s):<br>
    https://github.com/modelcontextprotocol/servers-archived/tree/main/src/brave-search<br>
    Follow the above directions and acquire an API Key<br><br>
    Brave Search configuration: src/main/resources/mcp-servers.json<br>
        Update with your BRAVE_API_KEY
```bash
npm i @modelcontextprotocol/server-brave-search
```

3. Setup Elastic Search:<br>
    Elastic Search MCP: https://github.com/elastic/mcp-server-elasticsearch<br><br>
    Install Elastic Search and Kibana: https://www.elastic.co/docs/deploy-manage/deploy/self-managed/install-kibana<br>
    Directions for loading a csv file into Elastic Search using Kibana.<br>
        https://www.elastic.co/blog/importing-csv-and-log-data-into-elasticsearch-with-file-data-visualizer<br><br>
    Use the following data to load an Elastic Index: mcp-human-resources/employee_code_of_conduct_policies.csv<br>
    Name the index: employee_code_of_conduct_policies<br><br>
    Configuration: src/main/resources/mcp-servers.json<br>
        Note that a space was needed in "ES_API_KEY": " ", since my local Elastic Index has not security enabled<br>

```bash
npm i @elastic/mcp-server-elasticsearch
```

4. Setup your preferred LLM, tested with GROQ and Google Vertex AI:<br>
    Spring AI supported models: https://docs.spring.io/spring-ai/reference/api/index.html

    Current code setup with GROQ<br>
    Acquire an API KEY: https://console.groq.com/keys

```bash
export GROQ_API_KEY=<YOUR_GROQ_API_KEY>
```

5.  Notes from when run with Google Vertex AI<br>
        https://docs.spring.io/spring-ai/reference/api/chat/vertexai-gemini-chat.html<br>	
		Including the following command run in the terminal that also starts the spring-boot app.<br>
			gcloud config set project <YOUR_PROJECT_ID> && gcloud auth application-default login <YOUR_ACCOUNT> <br><br>
		Need to set up ADC. (Need to run on a machine with a browser)<br>
			Helped with seting up ADC locally: <br>
				https://cloud.google.com/docs/authentication/set-up-adc-local-dev-environment
				https://www.youtube.com/watch?v=mEsC0BpEYGM<br>
			gcloud auth application-default login<br>
			In the browser when prompted choose my google user and then selected all access options.<br>
			Terminal then showed: Credentials saved to file: [/home/davidbrown/.config/gcloud/application_default_credentials.json]<br>
			First test resulted in an error and instructions with url link to resolve. I needed to follow the link and enable Vertex AI API<br><br>
            If switching from current GROQ setup to Google Vertex AI you will have to make comment/uncomment changes to pom.xml and application.properties 


6. Setup log directory and file
```bash
sudo mkdir /var/log/mcp-human-resources
sudo touch /var/log/mcp-human-resources/mcp-human-resources.log
sudo chmod -R 777 /var/log/mcp-human-resources
```
7. Run a Maven Intall<br>
```bash
"/home/<YOUR_HOME_DIRECTORY>/Documents/projects/mcp-human-resources/mvnw" install -f "/home/<YOUR_HOME_DIRECTORY>/Documents/projects/mcp-human-resources/pom.xml"
```

8. Start the server<br>
Update run.sh with your JDK install location
```bash
./run.sh
```

9. Can be tested using a browser or Postman<br><br>
    http://localhost:8081/ai?prompt=Write%20a%20few%20paragraphs%20about%20the%20Fermi%20Paradox%20and%20what%20are%20some%20of%20the%20possible%20explainations%20for%20why%20it%20exists.<br><br>
    http://localhost:8081/employees/5012<br><br>
    http://localhost:8081/ai?prompt=How%20many%20employees%20in%20IT%20are%20asian?<br><br>
    http://localhost:8081/ai/chat-response?prompt=I%20am%20visiting%20Baltimore%20Maryland%20next%20week,%20give%20me%20a%20list%20of%20twenty%20places%20to%20visit.<br><br>
    http://localhost:8081/ai/stream?prompt=I%20am%20visiting%20Baltimore%20Maryland%20next%20week,%20give%20me%20a%20list%20of%20twenty%20places%20to%20visit.<br><br>
    Can be tested using the associated Client APP:<br>
    https://github.com/dbrown725/mcp-human-resources-client

10. Uses H2 as a database, it is loaded on application startup.<br>
http://localhost:8081/h2-console<br>
Password is in application.properties<br>
Sample query: Select * from EMPLOYEE where AGE > 50;   

11. To see current logging level:<br>
http://localhost:8081/log