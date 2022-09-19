package tasks

import contributors.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) = coroutineScope {
  log("starting loading...")
  val repos = service.getOrgRepos(req.org).also { logRepos(req, it) }.body() ?: listOf()

  val channel = Channel<List<User>>()
  repos.forEach { repo ->
    launch {
      log("starting loading for ${repo.name}")
      channel.send(
          service.getRepoContributors(req.org, repo.name).also { logUsers(repo, it) }.bodyList())
    }
  }
  var allUsers = emptyList<User>()
  repeat(repos.size) { index ->
    val users = channel.receive()
    allUsers = (allUsers + users).aggregate()
    updateResults(allUsers, index == repos.lastIndex)
  }
}
