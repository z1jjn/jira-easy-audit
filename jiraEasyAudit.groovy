import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.groups.GroupManager
import com.atlassian.jira.security.login.LoginManager
import com.atlassian.jira.user.util.UserManager
import java.time.LocalDateTime
import java.time.ZoneId
import com.atlassian.jira.timezone.TimeZoneManager
import java.text.SimpleDateFormat
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import com.atlassian.jira.bc.group.search.GroupPickerSearchService

@BaseScript CustomEndpointDelegate delegate
jiraEasyAudit(httpMethod: "GET", groups: ["jira-administrators"]) { MultivaluedMap queryParams, String body ->
SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm aa z, E");
sdf.setTimeZone(ComponentAccessor.getComponent(TimeZoneManager).getLoggedInUserTimeZone())
UserManager userManager = ComponentAccessor.getComponent(UserManager.class)
GroupManager groupManager = ComponentAccessor.getComponent(GroupManager.class)
LoginManager loginManager = ComponentAccessor.getComponent(LoginManager.class)
GroupPickerSearchService groupPickerSearchService = ComponentAccessor.getComponent(GroupPickerSearchService)
def inactiveUsers = new StringBuilder()
def inactiveUsersList = new StringBuilder()
int inactiveUsersCount = 1
inactiveUsers <<= "<table><tr><th>#</th><th>ID</th><th>Name</th><th>Last Successful Login</th><th>Last Failed Login</th></tr>"
def neverLoggedIn = new StringBuilder()
def neverLoggedInList = new StringBuilder()
int neverLoggedInCount = 1
neverLoggedIn <<= "<table><tr><th>#</th><th>ID</th><th>Name</th></tr>"
def unusedGroups = new StringBuilder()
int unusedGroupsCount = 1
unusedGroups <<= "<table><tr><th>#</th><th>Group Name</th></tr>"
    
def groupList = groupPickerSearchService.findGroups("")//groupManager.getAllGroups()
def groupSize = groupList.size()
    
int counter = 0
while (counter < groupSize)
{
    if (groupManager.getUsersInGroup(groupList[counter]).size() == 0)
    {
        unusedGroups <<= "<tr><td>"
        unusedGroups <<= unusedGroupsCount++
        unusedGroups <<= "</td><td>"
        unusedGroups <<= groupList[counter].name
        unusedGroups <<= "</td></tr>"
    }
    counter++
}
unusedGroups <<= "</table>"
def users = groupManager.getUserNamesInGroup("jira-users") 
users.each{ user ->
def lastLoginAttempt = loginManager.getLoginInfo(user).getLastLoginTime()
def lastFailedLoginAttempt = loginManager.getLoginInfo(user).getLastFailedLoginTime()
if (lastLoginAttempt && lastFailedLoginAttempt)
    {
        def lastSuccessfulLogin = new Date(lastLoginAttempt).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        def lastFailedLogin = new Date(lastFailedLoginAttempt).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        def oneYearAgo = LocalDateTime.now().minusMonths(6)
         if (lastSuccessfulLogin?.isBefore(oneYearAgo) && lastFailedLogin?.isBefore(oneYearAgo))
        	{
                inactiveUsers <<= "<tr><td>"
                inactiveUsers <<= inactiveUsersCount++
                inactiveUsers <<= "</td><td>"
                inactiveUsers <<= "${user}"
                inactiveUsers <<= "</td><td>"
                inactiveUsers <<= "${userManager.getUserByName(user).getDisplayName()}"
                inactiveUsers <<= "</td><td>"
                inactiveUsers <<= "${sdf.format(new Date(lastLoginAttempt))}"
                inactiveUsers <<= "</td><td>"
                inactiveUsers <<= "${sdf.format(new Date(lastFailedLoginAttempt))}"
                inactiveUsers <<= "</td></tr>"
                inactiveUsersList <<= "${user}, "
         	}
      }
if (lastLoginAttempt && !lastFailedLoginAttempt)
    {
        def lastSuccessfulLogin = new Date(lastLoginAttempt).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        def sixMonthsAgo = LocalDateTime.now().minusMonths(6)
         if (lastSuccessfulLogin?.isBefore(sixMonthsAgo))
        	{
             	inactiveUsers <<= "<tr><td>"
                inactiveUsers <<= inactiveUsersCount++
                inactiveUsers <<= "</td><td>"
                inactiveUsers <<= "${user}"
                inactiveUsers <<= "</td><td>"
                inactiveUsers <<= "${userManager.getUserByName(user).getDisplayName()}"
                inactiveUsers <<= "</td><td>"
                inactiveUsers <<= "${sdf.format(new Date(lastLoginAttempt))}"
                inactiveUsers <<= "</td><td>"
                inactiveUsers <<= "No failed login attempt."
                inactiveUsers <<= "</td></tr>"
                inactiveUsersList <<= "${user}, "
         	}
      }
if (!lastLoginAttempt && lastFailedLoginAttempt)
    {
        def lastFailedLogin = new Date(lastFailedLoginAttempt).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        def sixMonthsAgo = LocalDateTime.now().minusMonths(6)
         if (lastFailedLogin?.isBefore(sixMonthsAgo))
        	{
             	inactiveUsers <<= "<tr><td>"
                inactiveUsers <<= inactiveUsersCount++
                inactiveUsers <<= "</td><td>"
                inactiveUsers <<= "${user}"
                inactiveUsers <<= "</td><td>"
                inactiveUsers <<= "${userManager.getUserByName(user).getDisplayName()}"
                inactiveUsers <<= "</td><td>"
                inactiveUsers <<= "Never successfully logged in."
                inactiveUsers <<= "</td><td>"
                inactiveUsers <<= "${sdf.format(new Date(lastFailedLoginAttempt))}"
                inactiveUsers <<= "</td></tr>"
                inactiveUsersList <<= "${user}, "
         	}
      }
if (!lastLoginAttempt && !lastFailedLoginAttempt)
    {
        neverLoggedIn <<= "<tr><td>"
        neverLoggedIn <<= neverLoggedInCount++
        neverLoggedIn <<= "</td><td>"
        neverLoggedIn <<= "${user}<br>"
        neverLoggedIn <<= "</td><td>"
        neverLoggedIn <<= "${userManager.getUserByName(user).getDisplayName()}"
        neverLoggedIn <<= "</td></tr>"
        neverLoggedInList <<= "${user}, "
    }
}
inactiveUsers <<= "</table>"
neverLoggedIn <<= "</table>"
def jiraEasyAuditDialog = """
<head>
<style>
table {
  border-collapse: collapse;
  width: 100%;
}
th, td {
  padding: 8px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}
tr:hover {background-color:#f5f5f5;}
html {
  scroll-behavior: smooth;
}
body {
 font-family: -apple-system, "Helvetica Neue", "Helvetica", "Arial";
 text-align: justify;
}
a:link {
color: #4788f4 ;
}
a:visited {
color: #654ea3 ;
}
a:hover {
color: #1089ff ;
}
p {
color:#302e30;
}
h1,h2,h3,h4,h5 {
 font-family: -apple-system-headline, "Helvetica Neue", "Helvetica", "Arial";
  font-weight: 700;
}
/* CHROME SCROLL BAR */
.scroll {
  width: 20px;
  height: 200px;
  overflow: auto;
  float: left;
  margin: 0 10px;
}
::-webkit-scrollbar {
  width: 5px;
}
::-webkit-scrollbar-track {
  background: #ddd;
}
::-webkit-scrollbar-thumb {
  background: #666; 
}
/*FOR FIREFOX*/
.scroller {
  width: 20px;
  height: 200px;
  overflow: auto;
  float: left;
  margin: 0 10px;
  scrollbar-color: #666 #ddd;
  scrollbar-width: thin;
}
</style>
<title>Jira Easy Audit</title>
<link rel="shortcut icon" href="REPLACE WITH SHORTCUT ICON LINK">
</head>
<body>
<h1>Jira Easy Audit</h1>
<p>Easily view active Jira users, enabled users who haven't logged in for 6 months, users who never logged in, and groups with no users.</p>
<ul>
<li><a href="#inactiveUsers">Inactive Users</a> Count: ${(inactiveUsersCount - 1)}</li>
<li><a href="#neverLoggedIn">Never Logged In</a> Count: ${(neverLoggedInCount - 1)}</li>
<li><a href="#unusedGroups">Unused Groups</a> Count: ${(unusedGroupsCount - 1)}</li>
</ul>
<h2><a id="inactiveUsers"></a>Inactive Users</h2>
${inactiveUsers}<br>${inactiveUsersList}
<h2><a id="neverLoggedIn"></a>Never Logged In</h2>
${neverLoggedIn}<br>${neverLoggedInList}
<h2><a id="unusedGroups"></a>Unused Groups</h2>
${unusedGroups}<br>
<p style='font-size:12px;text-align:right;'>Jira Easy Audit 1.1.0-stable.</p>
</body>
"""
Response.ok().type(MediaType.TEXT_HTML).entity(jiraEasyAuditDialog.toString()).build()
}