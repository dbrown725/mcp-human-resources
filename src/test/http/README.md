# HTTP Test Files

This directory contains `.http` files for testing the mcp-human-resources API endpoints directly from VS Code or IntelliJ IDEA.

## Prerequisites

- **VS Code**: Install the [REST Client extension](https://marketplace.visualstudio.com/items?itemName=humao.rest-client)
- **IntelliJ IDEA**: Built-in support for `.http` files

## Usage

1. Open any `.http` file in this directory
2. Click the "Send Request" link that appears above each request
3. View the response in a new tab/panel

## Variables

The base URL is defined at the top of each file:
```
@baseUrl = http://localhost:8081
```

You can modify this to point to different environments if needed.

## File Organization

- `ai.http` - AI chat and model endpoints
- `employee.http` - Employee data endpoints
- `file-storage.http` - File upload, download, delete operations
- `image.http` - Image generation and processing
- `email.http` - Email draft and inbox operations
- `rag.http` - RAG (Retrieval Augmented Generation) endpoints
- `weather.http` - Weather forecast and alerts
- `memory.http` - Chat memory endpoints

## Tips

- Requests are separated by `###`
- You can run requests sequentially or individually
- Comments start with `#`
- Use variables for repeated values
- For POST requests with file uploads, the file path is relative to your workspace

## More Information

- [REST Client documentation](https://marketplace.visualstudio.com/items?itemName=humao.rest-client)
- [IntelliJ HTTP Client](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)
