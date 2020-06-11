package nl.tudelft.hyperion.plugin.visualization.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import git4idea.GitUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import nl.tudelft.hyperion.plugin.git.GitLineTracker
import nl.tudelft.hyperion.plugin.git.GitVersionResolver
import nl.tudelft.hyperion.plugin.git.OriginBlameReadResult
import nl.tudelft.hyperion.plugin.settings.HyperionSettings
import nl.tudelft.hyperion.plugin.visualization.VisToolWindowFactory
import nl.tudelft.hyperion.plugin.visualization.errorDialog

class OpenLineGraphAction : AnAction() {
    companion object {
        // Updated when a gutter icon action is invoked
        // This is necessary due to action group popups resulting from gutter
        // icons not being bound to the line position, so it must be manually
        // stored somewhere to locate the originating line
        var cachedLogicalLine: Int? = null
    }

    @SuppressWarnings("TooGenericExceptionCaught")
    override fun actionPerformed(e: AnActionEvent) {
        val currentProject = e.getData(CommonDataKeys.PROJECT)
            ?: kotlin.run {
                errorDialog { "Current open project does not exist" }
                return@actionPerformed
            }
        val currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: kotlin.run {
                errorDialog { "Current open file is not linked to a project" }
                return@actionPerformed
            }

        // If the action was triggered from the gutter, use the cached line number
        // Otherwise use the line number at the caret
        val lineNumber = if (e.place == "ICON_NAVIGATION_SECONDARY_BUTTON" && cachedLogicalLine != null) {
            cachedLogicalLine!!
        } else {
            val currentCaret = e.getData(CommonDataKeys.CARET)
                ?: run {
                    errorDialog { "Action was triggered outside of PSI context" }
                    return
                }
            currentCaret.logicalPosition.line
        }

        val originInfo =
            try {
                getLineOriginInfo(
                    lineNumber,
                    currentProject,
                    currentFile
                ) ?: run {
                    errorDialog { "Could not find origin of selected line in $currentFile" }
                    return@actionPerformed
                }
            } catch (e: Exception) {
                // XXX: Not optimal
                //  This should trigger in the event that no git repository is available for current file
                //  But since GitUtil#GitRepositoryNotFoundException is private for _some_ reason,
                //  this is left as a catch all
                errorDialog { e.localizedMessage }
            }

        if (!currentFile.path.startsWith(currentProject.basePath!!)) {
            errorDialog { "file $currentFile is not in project ${currentProject.name}" }
            return
        }

        // TODO: make path finding more robust
        // This might fail on remote projects
        val relativePath = currentFile.path.removePrefix("${currentProject.basePath!!}/")

        val hyperionSettings = HyperionSettings.getInstance(currentProject)

        // Set file path to current file
        hyperionSettings.state.visualization.filePath = relativePath
        hyperionSettings.state.visualization.fileOnly = true

        // Open tool window if it exists
        ToolWindowManager
            .getInstance(currentProject)
            .getToolWindow("Visualization")
            ?.show {
                VisToolWindowFactory.histogramTab.updateAllSettings()
                VisToolWindowFactory.histogramTab.queryAndUpdate(
                    // Increment because the API stores it as visible lines instead of logical line
                    lineNumber = (originInfo as OriginBlameReadResult).lastSeenLine + 1
                )
                VisToolWindowFactory.histogramTab.root.repaint()
            }
    }

    private fun getLineOriginInfo(
        lineNumber: Int,
        currentProject: Project,
        currentFile: VirtualFile
    ): OriginBlameReadResult? = runBlocking {
        withContext(Dispatchers.IO) {
            GitLineTracker.runOriginBlame(
                currentProject,
                GitUtil.getRepositoryForFile(currentProject, currentFile),
                currentFile,
                GitVersionResolver.getCurrentOriginCommit(currentProject)
                    ?: kotlin.run {
                        errorDialog { "Current branch does not have an origin" }
                        return@withContext null
                    },
                lineNumber
            )
        }
    }
}
