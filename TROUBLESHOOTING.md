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
