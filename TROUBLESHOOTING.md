# Troubleshooting

## Environment Variable Issues

If you get authentication or API errors, verify your environment variables are set:
```bash
echo $BRAVE_API_KEY
echo $GEMINI_API_KEY
echo $GMAIL_EMAIL_ADDRESS
# etc.
```

If they're not set, either:
- Run the appropriate script (`./start-run.sh` or `./start-debug.sh`) instead of starting manually
- Add them to your shell configuration (`~/.bashrc` or `~/.zshrc`) as described in Option B in the main README

## Permission Denied

If you get "Permission denied" when running scripts:
```bash
chmod +x start-run.sh
chmod +x start-debug.sh
chmod +x run.sh
```

## Port Already in Use

If port 8081 is already in use:
```bash
# Find what's using the port
lsof -i :8081

# Kill the process (replace PID with actual process ID)
kill -9 <PID>
```

## Known Benign Startup Warnings

Some warnings can appear during local startup/restart (especially with DevTools) and are expected for this project setup.

- MCP framework startup warnings (tool/sampling/elicitation availability) can appear when optional capabilities are not registered yet.
- Bean post-processor checker warnings from Spring MCP auto-configuration may appear during early bean lifecycle phases.
- H2 shutdown warnings like `Database is already closed` can occur during rapid restart/shutdown cycles.

### When to investigate immediately

- Any `APPLICATION FAILED TO START` block.
- Missing required service bean errors (for example unresolved tool/service bean dependencies).
- Repeated `ERROR` logs from application packages (`com.megacorp...`) that persist after restart.

### Suggested workflow

1. Run a clean compile: `./mvnw -q -DskipTests clean compile`
2. Restart once with `./start-debug.sh`
3. If startup still fails, capture the first `APPLICATION FAILED TO START` block and investigate from that root error.
