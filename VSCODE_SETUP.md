# VS Code Setup and Tips

## Required Extensions
- **Extension Pack for Java** (Microsoft) - Essential for Java development and debugging
- **Spring Boot Extension Pack** (VMware) - Helpful for Spring Boot development
- **REST Client** (humao.rest-client) - To run `.http` test files

## Debugging Tips

**Breakpoint not working?** Make sure you:
1. Started the server with `./start-debug.sh` (not `./start-run.sh`)
2. Attached the VS Code debugger using "Attach to Debug Server (Port 5005)"
3. Set breakpoints on executable lines (not comments or blank lines)

**Server won't start?** Check that:
1. All environment variables are set correctly in the script files
2. Port 8081 is not already in use: `lsof -i :8081`
3. Your JDK is properly installed: `java -version`

**Debug connection refused?** Ensure:
1. The server is actually running (check terminal output)
2. Debug is enabled on the correct port (5005)
3. No firewall is blocking port 5005

## Terminal Navigation Reminder

Examples of navigating to the project root from any directory:
```bash
cd ~/Documents/projects/mcp-human-resources
```

Or use absolute path:
```bash
cd /home/<YOUR_HOME_DIRECTORY>/Documents/projects/mcp-human-resources
```
