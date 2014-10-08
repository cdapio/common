# Cask Common: HTTP

**Introduction**

The HTTP module contains a simple wrapper around HttpURLConnection for making HTTP requests.

## Usage

```
URL url = new URL("http://google.com");
HttpRequest request = HttpRequest.get(url).build();
HttpResponse response = HttpRequests.execute(request);
// do something with response
```
