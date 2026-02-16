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
    Follow the above directions and acquire an API Key<br>
    
```bash
export BRAVE_API_KEY=<YOUR_BRAVE_API_KEY>
```

3. Setup Google Cloud Storage:
    Used as a guide: https://github.com/sohamkamani/java-gcp-examples/blob/main/src/main/java/com/sohamkamani/storage/App.java
    // https://www.youtube.com/watch?v=FXiV4WPQveY
    ```bash
    export GEMINI_PROJECT_ID=<YOUR_GEMINI_PROJECT_ID>
    export STORAGE_BUCKET_NAME=<YOUR_STORAGE_BUCKET_NAME>
    ```
4. Needed for Nano Banana image generation
    ```bash
    export GEMINI_API_KEY=<YOUR_GEMINI_API_KEY>
    ```
5. Setup Elastic Search:<br>
    Elastic Search MCP: https://github.com/elastic/mcp-server-elasticsearch<br><br>
    Install Elastic Search and Kibana: https://www.elastic.co/docs/deploy-manage/deploy/self-managed/install-kibana<br>
    Directions for loading a csv file into Elastic Search using Kibana.<br>
        https://www.elastic.co/blog/importing-csv-and-log-data-into-elasticsearch-with-file-data-visualizer<br><br>
    Use the following data to load an Elastic Index: mcp-human-resources/employee_code_of_conduct_policies.csv<br>
    Name the index: employee_code_of_conduct_policies<br><br>
    Configuration: src/main/resources/mcp-servers.json<br>
        Note that a space was needed in "ES_API_KEY": " ", since my local Elastic Index does not have security enabled<br>

    For ElasticSearch RAG Ingest use later, navigate to http://localhost:5601/app/dev_tools and run "PUT /spring-ai-document-index"

```bash
npm i @elastic/mcp-server-elasticsearch
```

6. Setup your preferred LLMs, tested with GROQ, OPENROUTER and Google Vertex AI. Image Detection works with Google only, Gemini Flash 2.5 specifically:<br>
    Spring AI supported models: https://docs.spring.io/spring-ai/reference/api/index.html<br>
    Current code setup with Google Vertex AI as the Primary model/chatClient and two OpenAi compliant models as Secondary and Tertiary models/chatClients<br>
    Acquire an API KEY: https://console.groq.com/keys<br>
    Acquire an OPENROUTER KEY: https://openrouter.ai/settings/keys

```bash
export GROQ_API_KEY=<YOUR_GROQ_API_KEY>
export OPENROUTER_API_KEY=<YOUR_OPENROUTER_API_KEY>
```

7.  Notes on Google Vertex AI<br>
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


8. Setup GMAIL access<br>
    Email configuration, currently only Save Draft functionality exists.<br>
    Password in NOT your normal gmail password, the password needs to be an APP password: https://support.google.com/mail/answer/185833?hl=en
    ```bash
    export GMAIL_EMAIL_ADDRESS=<GMAIL_ADDRESSS>
    export GMAIL_EMAIL__APP_PASSWORD=<GMAIL_APP_PASSWORD>
    ```

9. Setup log directory and file
```bash
sudo mkdir /var/log/mcp-human-resources
sudo touch /var/log/mcp-human-resources/mcp-human-resources.log
sudo touch /var/log/mcp-human-resources/mcp-human-resources-call-advisor.log
sudo chmod -R 777 /var/log/mcp-human-resources
```

10. Setup and configure Prometheus and Grafana on Docker
    Metrics and Tracing Configuration. Based on https://github.com/danvega/spring-ai-metrics and https://youtu.be/FzLABAppJfM?t=17141<br><br>
    Prometheus: http://localhost:9090<br>
    Grafana: http://localhost:3000 <br><br>
    I had to update .../mcp-human-resources/docker/prometheus.yml to point to port 8081 instead of 8080 since this app runs on 8081.<br><br>
    I also had to <br>
          - go to http://localhost:3000/connections/datasources<br>
          - click on the Prometheus datasource<br>
          - change the name from 'Prometheus' to 'prometheus'<br>
          - click 'Save & Test' to make sure Grafana can connect to Prometheus<br><br>
    I also changed the admin password for Grafana in compose.yml to 'mysecretpassword' from 'admin' for better security. <br>
      if admin/mysecretpassword doesn't work, change it back to admin/admin

11. Rag elasticsearch data load. See ### RAG ingest document test case found in the rag.http file. Download the pdf, uncomment the test, set the path value to your download location and running the test will load the ElasticSearch index.      

12. Run a Maven Install<br>
```bash
"/home/<YOUR_HOME_DIRECTORY>/Documents/projects/mcp-human-resources/mvnw" install -f "/home/<YOUR_HOME_DIRECTORY>/Documents/projects/mcp-human-resources/pom.xml"
```

13. Start the server<br>
Update run.sh with your JDK install location
```bash
./run.sh
```

14. TESTS

    Run unit tests<br><br>
        Navigate to project's root directory and run "mvn test"<br><br>

    Run HTTP API tests<br><br>
        **Recommended**: Use the `.http` files in `src/test/http/` directory<br>
        - Install [REST Client extension](https://marketplace.visualstudio.com/items?itemName=humao.rest-client) for VS Code<br>
        - Or use IntelliJ IDEA's built-in HTTP client<br>
        - Open any `.http` file and click "Send Request" above each endpoint<br>
        - See `src/test/http/README.md` for detailed instructions<br><br>
        
        **Alternative**: Use a browser, curl, or Postman<br>
        - Example: http://localhost:8081/employees/5012<br>
        - Example: http://localhost:8081/ai?prompt=How%20many%20employees%20in%20IT%20are%20asian?<br><br>

    Can be tested using the associated Client APP:<br>
    https://github.com/dbrown725/mcp-human-resources-client

15. Uses H2 as a database, it is loaded on application startup.<br>
http://localhost:8081/h2-console<br>
Password is in application.properties<br>
Sample query: Select * from EMPLOYEE where AGE > 50;   

16. To see current logging level:<br>
http://localhost:8081/log