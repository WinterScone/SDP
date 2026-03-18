
# TODO: authentication #

Server-side sessions:  
- Spring boot HttpSession.  
- Log in >> store username/role in session.  
- API request >> check session server-side.  

Cookies:  
- Login >> set cookie  
- Logout >> clear cookie  
- API request >> validate cookie  
- TODO: update logout JS to call logout endpoint instead of clearing localStorage.  
- TODO: remove localStorage because cookie handles it.  


