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


8. Setup log directory and file
```bash
sudo mkdir /var/log/mcp-human-resources
sudo touch /var/log/mcp-human-resources/mcp-human-resources.log
sudo touch /var/log/mcp-human-resources/mcp-human-resources-call-advisor.log
sudo chmod -R 777 /var/log/mcp-human-resources
```

9. Setup and configure Prometheus and Grafana on Docker
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

10. Run a Maven Install<br>
```bash
"/home/<YOUR_HOME_DIRECTORY>/Documents/projects/mcp-human-resources/mvnw" install -f "/home/<YOUR_HOME_DIRECTORY>/Documents/projects/mcp-human-resources/pom.xml"
```

11. Start the server<br>
Update run.sh with your JDK install location
```bash
./run.sh
```

12. TESTS

    Run unit tests<br><br>
        Navigate to project's root directory and run "mvn test"<br><br>

    Run http tests<br><br>
        Can be tested using a browser or Postman<br><br>
        http://localhost:8081/ai?prompt=Write%20a%20few%20paragraphs%20about%20the%20Fermi%20Paradox%20and%20what%20are%20some%20of%20the%20possible%20explainations%20for%20why%20it%20exists.<br><br>
        http://localhost:8081/employees/5012<br><br>
        http://localhost:8081/ai?prompt=How%20many%20employees%20in%20IT%20are%20asian?<br><br>
        http://localhost:8081/ai/chat-response?prompt=I%20am%20visiting%20Baltimore%20Maryland%20next%20week,%20give%20me%20a%20list%20of%20twenty%20places%20to%20visit.<br><br>
        http://localhost:8081/ai/stream?prompt=I%20am%20visiting%20Baltimore%20Maryland%20next%20week,%20give%20me%20a%20list%20of%20twenty%20places%20to%20visit.<br><br>
        http://localhost:8081/receipt-image-to-text?prompt=Write%20a%20summary%20of%20the%20receipt%20contents
        (see ImageDetectionController.java)<br><br>
        http://localhost:8081/receipt-image-to-text?prompt=What%20is%20the%20Total%20Amount%20Due?<br><br>
        http://localhost:8081/receipt-image-to-text?prompt=Write%20a%20summary%20of%20the%20receipt%20contents.<br><br>
        http://localhost:8081/read-file?fileName=user_data/sample_user_data.txt<br><br>
        http://localhost:8081/list-files?prefix=expense_receipts/<br><br>
        POST http://localhost:8081/upload<br><br>
        In POSTMAN the key should be "file" and the value field clicked should allow you to add a file from your file system, for example george_bush.jpg.<br><br>
        http://localhost:8081/download-file/george_bush.jpg<br><br>
        http://localhost:8081/delete-file?fileName=george_bush.jpg<br><br>
        http://localhost:8081/delete-file?fileName=expense_receipts/20250831_20250913/expense_report.csv<br><br>
        http://localhost:8081/summarize-images-in-folder?folder=expense_receipts/20250831_20250913<br><br>
        http://localhost:8081/generate-expense-report?folder=expense_receipts/20250831_20250913<br><br>
        http://localhost:8081/generate-image?prompt=Create%20a%20picture%20of%20a%20Pho%20soup%20stall%20in%20the%20style%20of%20the%20movie%20Blade%20Runner.&outputImageRootName=phoRunner2<br><br>
        http://localhost:8081/generate-image?prompt=Create%20a%20picture%20of%20my%20cat%20eating%20a%20nano-banana%20in%20a%20fancy%20restaurant%20under%20the%20Gemini%20constellation.&optionalInputImageNames=original_images/cat-close-up-of-side-profile.webp&outputImageRootName=myCat<br><br>
        http://localhost:8081/generate-employee-badge?firstName=Randy&lastName=Broussard&employeeNumber=6000&existingEmployeeImageName=original_images/randy_broussard.jpg<br><br>
        http://localhost:8081/memory?message=My%20name%20is%20Bill%20Smith<br><br>
        http://localhost:8081/memory?message=What%20is%20my%20name?<br><br>
        http://localhost:8081/models/stuff-the-prompt<br><br>
        http://localhost:8081/rag/models<br><br>
        Rag elasticsearch data load.<br><br>
        Download https://www.terma.com/media/nf3ewqzn/employee-code-of-conduct-jan-2021.pdf<br><br>
        POST http://localhost:8081/rag/ingest<br><br>
        In POSTMAN the key should be "path" and the value field clicked should allow you to add a file from your file system, for example employee-code-of-conduct-policy.pdf.<br><br>
        http://localhost:8081/rag/query?question=Give%20me%20detailed%20information%20on%20Anti-Corruption%20Policy<br><br>
        http://localhost:8081/rag/advised?question=Give%20me%20detailed%20information%20on%20Ethical%20Decision%20Tree,%20Drugs%20and%20a%20Tobacco%20-%20Free%20Environment%20in%20the%20Workplace<br><br>
        http://localhost:8081/weather/forecast?message=Give%20me%20the%20weather%20forcast%20for%2025.791484,%20-80.129938<br><br>
        http://localhost:8081/weather/alerts?message=Give%20me%20weather%20alerts%20for%20Florida<br><br>
        http://localhost:8081/ai/model/secondary?prompt=Why%20do%20you%20park%20on%20a%20driveway%20,but%20drive%20on%20a%20parkway?<br><br>
        http://localhost:8081/ai/model/tertiary?prompt=Write%20a%20few%20paragraphs%20that%20describe%20the%20war%20of%201812.<br><br>

    Can be tested using the associated Client APP:<br>
    https://github.com/dbrown725/mcp-human-resources-client

13. Uses H2 as a database, it is loaded on application startup.<br>
http://localhost:8081/h2-console<br>
Password is in application.properties<br>
Sample query: Select * from EMPLOYEE where AGE > 50;   

14. To see current logging level:<br>
http://localhost:8081/log