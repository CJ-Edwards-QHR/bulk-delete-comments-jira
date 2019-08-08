import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.comments.Comment
import com.atlassian.jira.issue.comments.CommentManager
import com.atlassian.jira.issue.search.SearchException
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.user.ApplicationUsers
import com.atlassian.crowd.embedded.api.User
import com.atlassian.jira.jql.parser.JqlQueryParser
import org.apache.log4j.Logger
def log = Logger.getLogger("com.acme.workflows")

def userManager = ComponentAccessor.userManager

def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def searchService = ComponentAccessor.getComponent(SearchService)

def projectName = 'SOMEPROJECT'
def offendingUsername = 'tom.jones'
def keyPhrase = 'Great test h1ts'
def user = userManager.getUserByName(offendingUsername)

// build query
def query = jqlQueryParser.parseQuery("project = ${projectName} AND comment~\"${keyPhrase}\" AND issueFunction in commented(\"by ${offendingUsername}\")")
def search = searchService.search(user, query, PagerFilter.getUnlimitedFilter())

//execute query 
def issueList = search.getIssues()

// iterate through the list of issues found
// delete the matching issues via deleteCommentsMatching
issueList.each { issue -> 
        deleteCommentsMatching(keyPhrase, issue) { Comment comment ->
            offendingUsername.equalsIgnoreCase(comment.getAuthorApplicationUser().username)
        }
}

// nested lambda expressions hurt my brain.. 
// NOTES:
// Pass proper keyPhrase to pattern match each comment directly
// issue is immutable
def deleteCommentsMatching(String keyP, Issue issue, Closure commentFilter) {
   	def issueManager = ComponentAccessor.issueManager
    def commentManager = ComponentAccessor.commentManager

    // Current issue is immutable, recreate by pulling it directly
    Issue muissue = issueManager.getIssueObject(issue.getId())
    List<Comment> comments = commentManager.getComments(muissue)

    // grab the comments in the matched filter based on username found == offendingUsername
    def matchingComments = comments.findAll(commentFilter)
    
    // Compare the string of the comment body to make sure we dont delete relevant comments!!
    matchingComments.each { comment ->
        if (comment.getBody().contains(keyP)){        
           log.warn("Deleting comment containing: " + comment.getBody())
	       commentManager.delete(comment)
        }
    }
}

