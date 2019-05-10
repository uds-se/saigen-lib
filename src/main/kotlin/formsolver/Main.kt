package formsolver

/*import org.apache.jena.sparql.core.QuerySolutionBase
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties
import edu.washington.cs.knowitall.morpha.MorphaStemmer
import uk.ac.susx.informatics.Morpha

object Main {

	@JvmStatic
	fun main(args: Array<String>) {
		var internalArgs = args
		var kek = args
		//val dataCrowBestOrder = arrayOf("Isbn", "Series", "Title", "Year", "Volume", "Pages", "Description", "Comment", "Webpage")
		//val aMetro = arrayOf("Station", "Station")
		var i = 0
		val prova = arrayOfNulls<String>(6)
		prova[i++] = "author"
		prova[i++] = "publisher"
		prova[i++] = "isbn"
		prova[i++] = "genre"
		prova[i++] = "title"
		prova[i] = "serial"
		//val ldapWork = arrayOf("Job", "Organisation", "Country", "Address", "City", "County", "PostCode", "Department")
		//val ldapCont = arrayOf("FirstName", "LastName", "DisplayName", "Email", "AdditionalEmail", "WorkPhone", "HomePhone", "Fax", "Mobile")
		//val ldapContWork = arrayOf("name", "surname", "DisplayName", "Email", "AdditionalEmail", "WorkPhone", "HomePhone", "Fax", "Mobile", "Job", "Organisation", "Country", "Address", "City", "County", "PostCode", "Department")
		var myjlibrary = arrayOf("Isbn", "Title", "Publisher", "Author", "Published", "Description", "Language", "Pages")
		//val flight = arrayOf("Airport", "Airline")
		//myjlibrary = arrayOf("Isbn", "Book", "Author", "Genre")
		myjlibrary = arrayOf("Street", "City")
		myjlibrary[0] = "State"
		val sogliaAssociazioni = 50
		val usoSoglia = false
		val euristica = 2
		val prop = Properties()
		var input: FileInputStream? = null
		try {
			input = FileInputStream("config.properties")
			prop.load(input)
			if (prop.getProperty("endpoint") != null) {
				FormSolver.endpoint = prop.getProperty("endpoint")
			}
			if (prop.getProperty("labels") != null) {
				internalArgs = prop.getProperty("labels").split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

			}
		} catch (ex: IOException) {
			ex.printStackTrace()
			System.exit(-1)
		}

		if (internalArgs.isEmpty()) {
			println("Missing inputs")
			return
		}
		var etichette = internalArgs
		while (etichette.isNotEmpty()) {
			val risolutore = FormSolver(etichette, usoSoglia, sogliaAssociazioni, euristica)
			val etichetteTagliate = risolutore.runProcess(1)

			etichette = etichetteTagliate.toTypedArray()
		}
		i = 0
		while (i < CommonData.outputQueries.size) {
			val query2 = CommonData.outputQueries[i]
			println("*********************************")
			println("*********************************")
			println("*********************************")
			println(query2)
			println("i: $i")
			++i
		}

		val aaa = edu.washington.cs.knowitall.morpha.MorphaStemmer.stem("cities dogs and cats need to stayed together for the stars. addresses are ten twenty fives. geese are cool. lets go to parties")


		val bbb = uk.ac.susx.informatics.Morpha.noun

		println("++++++++++++++++++++HERE IS THE aaa: $aaa")


		val pairy = Pair("adogo", "aCATDO")
		println(pairy)
		//println("http://dbpedia.org/resource/Rock_music".replaceBeforeLast("/", ""))
		//println("http://dbpedia.org/resource/Rock_music".substringAfterLast("/"))
		//println("http://dbpedia.org/resource/Rock_music".substringAfterLast("/").replace("_"," "))
	}
}*/