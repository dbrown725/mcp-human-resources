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

**Note:** If using the convenience scripts (`start-run.sh` or `start-debug.sh`), you don't need to run this export command manually. Just edit the script files and set your API key there.<br>

3. Setup Google Cloud Storage:
    Used as a guide: https://github.com/sohamkamani/java-gcp-examples/blob/main/src/main/java/com/sohamkamani/storage/App.java
    // https://www.youtube.com/watch?v=FXiV4WPQveY
    ```bash
    export GEMINI_PROJECT_ID=<YOUR_GEMINI_PROJECT_ID>
    export STORAGE_BUCKET_NAME=<YOUR_STORAGE_BUCKET_NAME>
    ```
    
**Note:** If using `start-run.sh` or `start-debug.sh`, set these values in the script files instead of exporting manually.<br><br>
4. Needed for Nano Banana image generation
    ```bash
    export GEMINI_API_KEY=<YOUR_GEMINI_API_KEY>
    ```
    
**Note:** Set this in `start-run.sh` or `start-debug.sh` if using those scripts.<br><br>
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

**Note:** Set these in `start-run.sh` or `start-debug.sh` for automatic configuration.<br>

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


8. Setup GMAIL access<br>
    Email configuration, currently only Save Draft functionality exists.<br>
    Password is NOT your normal gmail password, the password needs to be an APP password: https://support.google.com/mail/answer/185833?hl=en
    ```bash
    export GMAIL_EMAIL_ADDRESS=<GMAIL_ADDRESSS>
    export GMAIL_EMAIL__APP_PASSWORD=<GMAIL_APP_PASSWORD>
    ```
    
**Note:** Set these in `start-run.sh` or `start-debug.sh` - these scripts handle all environment variable configuration.<br>

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
"./mvnw install"
```

13. Start the server<br>

### Option A: Using the convenience scripts (Recommended for VS Code development)<br>

The repository includes two convenience scripts that automatically set all required environment variables and start the server:

#### Step 1: Make the scripts executable<br>

Navigate to the project root directory and run:
```bash
cd /home/<YOUR_HOME_DIRECTORY>/Documents/projects/mcp-human-resources
chmod +x start-run.sh
chmod +x start-debug.sh
```

#### Step 2: Run the appropriate script<br>

**For normal development:**<br>
```bash
./start-run.sh
```

**For debugging with VS Code breakpoints:**<br>
```bash
./start-debug.sh
```

The scripts automatically export all environment variables listed above (BRAVE_API_KEY, GEMINI_PROJECT_ID, STORAGE_BUCKET_NAME, GEMINI_API_KEY, GROQ_API_KEY, OPENROUTER_API_KEY, GMAIL_EMAIL_ADDRESS, GMAIL_EMAIL_APP_PASSWORD).

**Important:** You need to edit `start-run.sh` and `start-debug.sh` to set your actual API keys and credentials before running them. Open each file and replace the placeholder values with your actual keys.

#### Step 3: (Debug mode only) Attach VS Code debugger<br>

When using `./start-debug.sh`, the server starts with remote debugging enabled on port 5005. To enable breakpoint debugging in VS Code:

1. Ensure your `.vscode/launch.json` contains an attach configuration:
```json
{
    "type": "java",
    "name": "Attach to Debug Server (Port 5005)",
    "request": "attach",
    "hostName": "localhost",
    "port": 5005,
    "projectName": "humanresources"
}
```

2. Open the **Run and Debug** panel in VS Code (`Ctrl+Shift+D` or `Cmd+Shift+D`)

3. Select **"Attach to Debug Server (Port 5005)"** from the dropdown at the top

4. Click the green play button (or press `F5`) to attach the debugger

5. Set breakpoints in your Java files - they will now work!

**Critical:** You must perform the "Attach to Debug Server" step **every time** you start the server with `./start-debug.sh`. If you don't attach the debugger, VS Code will ignore your breakpoints because it's not connected to the running Java process.

**Understanding the difference:**
- **"launch" configurations** in VS Code start a new Java process
- **"attach" configurations** connect to an already-running Java process
- The scripts use Maven to start the server externally, so you need "attach" mode to connect VS Code's debugger to that running process

### Option B: Setting environment variables permanently

If you don't want to use the scripts, you can set environment variables permanently in your shell configuration:

**For bash users** (most Linux systems), add the export statements to `~/.bashrc`:
```bash
nano ~/.bashrc
# Add all the export statements from start-run.sh or start-debug.sh
# Save and exit (Ctrl+X, then Y, then Enter)
source ~/.bashrc
```

**For zsh users** (default on macOS), add them to `~/.zshrc`:
```bash
nano ~/.zshrc
# Add all the export statements from start-run.sh or start-debug.sh
# Save and exit (Ctrl+X, then Y, then Enter)
source ~/.zshrc
```

After setting up permanent environment variables, you can start the server directly with Maven:
```bash
./mvnw spring-boot:run
```

Or for debug mode:
```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

### Option C: Using the original run.sh script

Update run.sh with your JDK install location and environment variables, then run:
```bash
./run.sh
```

## Additional Documentation

- **[VS Code Setup and Tips](VSCODE_SETUP.md)** - Required extensions, debugging tips, and terminal navigation
- **[Troubleshooting](TROUBLESHOOTING.md)** - Solutions for common issues with environment variables, permissions, and ports

14. TESTS

    Run unit tests<br><br>
        Navigate to project's root directory and run "./mvnw test"<br><br>

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