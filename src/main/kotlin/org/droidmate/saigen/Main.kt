package org.droidmate.saigen

import kotlinx.coroutines.runBlocking
import org.droidmate.api.ExplorationAPI
import org.droidmate.command.ExploreCommandBuilder
import org.droidmate.configuration.ConfigProperties
import org.droidmate.configuration.ConfigurationBuilder
import org.droidmate.configuration.ConfigurationWrapper
import org.droidmate.exploration.SelectorFunction
import org.droidmate.exploration.StrategySelector
import org.droidmate.saigen.storage.DictionaryProvider
import org.droidmate.saigen.storage.LinkProvider
import org.droidmate.saigen.storage.Storage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * Example run config:
 *
 * VM Options: -Dkotlinx.coroutines.debug -Dlogback.configurationFile=default-logback.xml
 * Args: --Selectors-randomSeed=0 --Selectors-actionLimit=1000 --DeviceCommunication-deviceOperationDelay=0 --UiAutomatorServer-waitForIdleTimeout=1000 --UiAutomatorServer-waitForInteractableTimeout=1000 --StatementCoverage-onlyCoverAppPackageName=true --StatementCoverage-enableCoverage=true --Deploy-replaceResources=true --Deploy-installAux=true --Deploy-installMonitor=false
 */
class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runBlocking {
                // debug()
                // System.exit(0)
                val cfg = ConfigurationBuilder().build(args)
                // val builder = ExploreCommandBuilder.fromConfig(cfg)

                // ExplorationAPI.explore(cfg, builder)

                /** *
                 * To get the coverage, app should first be instrumented
                 * To instrument uncomment the next lines
                 */
                /*CoverageCommand(cfg,
                    Instrumenter(cfg.resourceDir, cfg[StatementCoverageMF.Companion.StatementCoverage.onlyCoverAppPackageName], true)
                ).execute()

                System.exit(0)*/

                val saigenSelector: SelectorFunction = { eContext, pool, _ ->
                    val saigen by lazy { eContext.getOrCreateWatcher<SaigenMF>() }
                    saigen.join()

                    pool.getFirstInstanceOf(SaigenRandom::class.java)
                }

                val commandBuilder = ExploreCommandBuilder.fromConfig(cfg)
                    // Add custom strategies
                    .withStrategy(SaigenRandom(cfg.randomSeed))
                    .withStrategy(SaigenCAM(emptyList(), 0))
                    // Remove random and cannotExplore
                    .remove(StrategySelector.randomWidget)
                    // Add custom selector
                    // .append("CAM", camSelector, arrayOf(getCAMs()))
                    .append("SAIGEN", saigenSelector)

                ExplorationAPI.explore(
                    args,
                    commandBuilder = commandBuilder,
                    watcher = emptyList()
                )

                writeStatisticsToFile(cfg)
            }
        }

        // This method must be executed after SaigenMF.context was initialized. Kinda hacky but a good way to get baseDir.
        private fun writeStatisticsToFile(cfg:ConfigurationWrapper) {
            println("Writing stats.txt")

            val baseDir = SaigenMF.context.model.config.baseDir
            //val statisticsDir = Paths.get(cfg[ConfigProperties.Output.outputDir].path).toAbsolutePath().resolve("statistics").toAbsolutePath()
            val statisticsDir = baseDir.toAbsolutePath().resolve("statistics").toAbsolutePath()
            if (!Files.exists(statisticsDir))
                Files.createDirectories(statisticsDir)
            val statisticsFile = statisticsDir.resolve("stats.txt")
            Files.deleteIfExists(statisticsFile)
            Files.createFile(statisticsFile)

            var uniqueWidgets = mutableMapOf<UUID, Int>()
            SaigenMF.concreteIDMap.forEach { (key, value) ->
                if (!uniqueWidgets.containsKey(key.uid) || value != 0)
                    uniqueWidgets[key.uid] = value
            }

            Files.write(statisticsFile, ("#total input fields found: " + SaigenMF.concreteIDMap.size + "\n").toByteArray(), StandardOpenOption.APPEND)
            Files.write(statisticsFile, ("#total input fields filled automatically (DBPedia, DictionaryProvider): " + SaigenMF.concreteIDMap.filterValues { it==1  }.size + "\n").toByteArray(), StandardOpenOption.APPEND)

            Files.write(statisticsFile, ("#unique input fields found: " + uniqueWidgets.size + "\n").toByteArray(), StandardOpenOption.APPEND)
            Files.write(statisticsFile, ("#unique input fields filled automatically (DBPedia, DictionaryProvider): " + uniqueWidgets.filterValues { it==1  }.size + "\n").toByteArray(), StandardOpenOption.APPEND)
            //Files.write(statisticsFile, ("#unique input fields selected (fields that were filled by any method): " + SaigenMF.uidMap.filterValues { it.first==true  }.size + "\n").toByteArray(), StandardOpenOption.APPEND)
            // Files.write(statisticsFile, ("#unique input fields filled via DictProviders: ").toByteArray(), StandardOpenOption.APPEND)
            // Files.write(statisticsFile, ("#unique input fields filled randomly").toByteArray(), StandardOpenOption.APPEND)


            println("Writing querydebug.txt")
            val queryDebugFile = statisticsDir.resolve("querydebug.txt")
            Files.deleteIfExists(queryDebugFile)
            Files.createFile(queryDebugFile)

            Files.write(queryDebugFile, "Query debug information:\n".toByteArray(), StandardOpenOption.APPEND)
            SaigenMF.queryMap.forEach{ q ->
                Files.write(queryDebugFile, (q.key.second + " = {" + q.value.joinToString(",") + "}\n").toByteArray(), StandardOpenOption.APPEND)

                if (q.key.second in SaigenMF.allQueriedLabels) {
                    SaigenMF.allQueriedLabels.remove(q.key.second)
                }
            }

            Files.write(queryDebugFile, ("Labels for which we could not get any results: " + SaigenMF.allQueriedLabels + "\n").toByteArray(), StandardOpenOption.APPEND)
        }

        private fun getCAMs(): List<CAM> {
            // TODO Pending...
            // val loginCam = CAM(listOf("username", "password"), listOf("log in"))
            return emptyList()
        }

        private fun debug() {
            val link = Storage(sortedSetOf(LinkProvider(), DictionaryProvider(mapOf("name" to listOf("first name", "second name")))))

            val r = link.query(listOf("address", "name", "city", "email", "phone", "car"))

            r.forEach { result ->
                println("GroupId: ${result.queryId}\tLabel: ${result.label}\tValues: ${result.values.joinToString(" | ")}")
            }
        }

        /* Wordnik dict

        System.setProperty("WORDNIK_API_KEY", "72d81865c31c2912fd866055fc20c53890e8ff1b85a019f3d")

            val status = AccountApi.apiTokenStatus()
            if (status.isValid) {
                println("API key is valid.")
            } else {
                println("API key is invalid!")
                System.exit(1)
            }

            // get a list of definitions for a word
            val def = WordApi.definitions("siren")
            println("Found " + def.size + " definitions.")

            var i = 1
            for (d in def) {
                println(i++.toString() + ") " + d.partOfSpeech + ": " + d.text)
            }

            val syn = WordApi.related("siren", true, setOf(Knicker.RelationshipType.synonym), 50)
            println("Found " + syn.size + " synonyms.")
            i = 1
            for (d in syn.first().words) {
                println(i++.toString() + ") " + d)
            }
         */
    }
}