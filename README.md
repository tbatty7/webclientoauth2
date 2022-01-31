## Implementation of WebClient with token caching

### To Do:

- Replace deprecated tcpConfiguration method with new one
- I need to do more testing to confirm I can pass properties to bootRun

### Done:

- Switched to the new way of doing Spring Profiles with minor modifications
    - Just add the spring.profiles.active= in application.properties
    - Run tests with this command: ./gradlew build -Dspring.profiles.active=local
    - This will activate the spring profile defined in application-local.properties