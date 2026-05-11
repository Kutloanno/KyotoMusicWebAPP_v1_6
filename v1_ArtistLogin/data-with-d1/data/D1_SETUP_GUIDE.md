# Cloudflare D1 Integration with Spring Boot

This Spring Boot application is configured to connect to Cloudflare D1 database via the Cloudflare REST API.

## Setup Instructions

### 1. Get Your Cloudflare Credentials

#### Account ID:
1. Go to [Cloudflare Dashboard](https://dash.cloudflare.com)
2. Click on **Workers & Pages** (or any section)
3. Your Account ID is visible in the URL or right sidebar
4. Example: `https://dash.cloudflare.com/YOUR_ACCOUNT_ID/workers`

#### Database ID:
1. Go to **Storage & Databases** → **D1**
2. Click on your database
3. Copy the **Database ID** shown on the page

#### API Token:
1. Click your profile icon → **My Profile** → **API Tokens**
2. Click **Create Token**
3. Use the "Edit Cloudflare Workers" template OR create a custom token with:
   - Account → D1 → Edit permissions
4. Click **Continue to summary** → **Create Token**
5. **COPY THE TOKEN NOW** (you won't see it again!)

### 2. Configure application.properties

Edit `src/main/resources/application.properties` and replace:

```properties
cloudflare.account.id=YOUR_ACCOUNT_ID_HERE
cloudflare.database.id=YOUR_D1_DATABASE_ID_HERE
cloudflare.api.token=YOUR_API_TOKEN_HERE
```

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

Or in Windows:
```bash
mvnw.cmd spring-boot:run
```

### 4. Test the Connection

Once the app is running, test the connection:

```bash
curl http://localhost:8080/api/d1/test
```

You should see:
```json
{
  "status": "Connected to Cloudflare D1 successfully!",
  "result": [{"test": 1}]
}
```

## API Endpoints

### Test Connection
```
GET http://localhost:8080/api/d1/test
```

### Query a Table
```
GET http://localhost:8080/api/d1/query?table=users
```

### Execute Custom SQL
```
POST http://localhost:8080/api/d1/execute
Content-Type: application/json

{
  "sql": "SELECT * FROM users WHERE name = 'John'"
}
```

### Insert Record
```
POST http://localhost:8080/api/d1/insert
Content-Type: application/json

{
  "table": "users",
  "name": "John Doe",
  "email": "john@example.com"
}
```

### Update Record
```
PUT http://localhost:8080/api/d1/update
Content-Type: application/json

{
  "table": "users",
  "id": 1,
  "name": "Jane Doe",
  "email": "jane@example.com"
}
```

### Delete Record
```
DELETE http://localhost:8080/api/d1/delete?table=users&id=1
```

## Using D1Service in Your Code

```java
@Service
public class UserService {
    
    @Autowired
    private D1Service d1Service;
    
    public List<Map<String, Object>> getAllUsers() {
        D1Response response = d1Service.executeQuery("SELECT * FROM users");
        return d1Service.getResults(response);
    }
    
    public void createUser(String name, String email) {
        String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
        d1Service.executeUpdateWithParams(sql, List.of(name, email));
    }
}
```

## Important Notes

- **Security**: Never commit your API token to version control! 
- Consider using environment variables for production:
  ```bash
  export CLOUDFLARE_ACCOUNT_ID=your_account_id
  export CLOUDFLARE_DATABASE_ID=your_database_id
  export CLOUDFLARE_API_TOKEN=your_api_token
  ```
  
  Then update application.properties:
  ```properties
  cloudflare.account.id=${CLOUDFLARE_ACCOUNT_ID}
  cloudflare.database.id=${CLOUDFLARE_DATABASE_ID}
  cloudflare.api.token=${CLOUDFLARE_API_TOKEN}
  ```

- **Rate Limits**: Cloudflare D1 has API rate limits. Be mindful of query frequency.

- **SQL Injection Prevention**: Always use parameterized queries with `executeQueryWithParams()` or `executeUpdateWithParams()` when using user input.

## Troubleshooting

### "Unauthorized" Error
- Check your API token has D1 permissions
- Verify the token hasn't expired

### "Database not found" Error
- Double-check your Account ID and Database ID
- Make sure they're from the same Cloudflare account

### Connection Timeout
- Check your internet connection
- Verify Cloudflare API is accessible from your network

## Next Steps

1. Create your database schema in D1
2. Build your own services and controllers
3. Add proper error handling and validation
4. Implement authentication/authorization for your endpoints
