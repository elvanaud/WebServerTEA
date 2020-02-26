
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*; 
import java.util.*; 
import java.lang.*; 
public class WebServer{ 
	// comptage du nombre de sessions    
	static int nbSessions = 0; 
	// chaines de caracteres formant la reponse HTTP    
	static String serverLine = "Server: Simple Serveur de TP ULR";    
	static String statusLine = null;    
	static String contentTypeLine = null;    
	static String entityBody = null;    
	static String contentLengthLine = null; 
	// constante a positionner pour controler le niveau d'impressions 
	// de controle (utilisee dans la methode debug(s,n)    
	static final int DEBUG = 255; 
	// Lancement du serveur 
	// Le serveur est en boucle infinie, et ne s'arrete que si il y a une  
	// erreur d'Entree/Sortie. Il y a fermeture de socket apres chaque 
	// requete.     
	public static void go (int port){       
		Socket sck = null;       
		ServerSocket srvk;       
		DataOutputStream os = null;       
		BufferedReader br = null;       
		try {          
			srvk = new ServerSocket (port);          
			while (true){                      
				System.out.println("Serveur en attente "+(nbSessions++));
				sck = srvk.accept ();             
				os = new DataOutputStream (sck.getOutputStream ());
				br =  new BufferedReader(new InputStreamReader (sck.getInputStream())); 
				traiterRequete(br, os);    
				sck.close();      
			}        
		}           
		catch (IOException e) {
			System.out.println("ERREUR IO"+ e);        
		}                        
	} // go  
	
	// Methode utile pour demander ou non des print de Trace a l'execution         
	public static void debug(String s, int n) {       
		if ((DEBUG & n) != 0)          
			System.out.println("("+n+")"+s);  
	} // debug     
	
	public static String lireLigne(String p, BufferedReader br) throws IOException { 
		String s ;       
		s =br.readLine();  
		debug(p+" "+s,2);      
		return s;    
	} // lireLigne     
	
	public static void traiterRequete(BufferedReader br, DataOutputStream dos) throws IOException {       
		/*  Cette methode lit des lignes sur br (utiliser LireLigne) et recherche      
		 *  une ligne commencant par GET ou par POST.    
		 *  Si la ligne commence par GET,         
		 *      - on extrait le nom de fichier demande dans la ligne et on appelle la methode 
		 *      	retourFichier.          
		 *      - Si le suffixe du nom de fichier est .htm ou .html (utiliser la methode contentType) 
		 *      - on lit ensuite toutes les lignes qui suivent jusqu'a entrouver une vide, nulle ou contenant juste "\n\r"     
		 *  Si la ligne commence par POST        
		 *  	- on extrait le nom de fichier demande dans la ligne et onappelle la methode retourFichier. 
        		- Si le suffixe du nom de fichier est .htm ou .html, on fait la meme chose que ci-dessus pour GET         
        		- Si le suffixe est autre, on appelle la methode retourCGIPOST       */   
		String line = lireLigne("Premiere ligne de requete", br);
		if(line == null) {
			debug("Couldn't read line (null)",2);
			return;
		}
		if(line.substring(0, 3).contentEquals("GET"))
		{
			debug("Exec GET: " + line, 2);
			String[] split = line.split(" ");
			String filename = split[1];
			if(filename.startsWith("/"))
				filename = filename.substring(1);//strip the /
			debug("FileName: "+filename,2);
			if(filename.isEmpty() || filename.equals("/"))
			{
				filename = "index.html";
			}
			retourFichier(filename, dos);
		}
		else if(line.substring(0, 4).contentEquals("POST"))
		{
			debug("Exec POST",2);
			String[] split = line.split(" ");
			String filename = split[1];
			if(filename.startsWith("/"))
				filename = filename.substring(1);//strip the /
			if(filename.isEmpty())
			{
				filename = "index.html";
			}
			debug("FileName: "+filename,2);
			if(filename.endsWith(".html") || filename.endsWith(".htm"))
			{
				retourFichier(filename, dos);
			}
			else
			{
				debug("CGI post",2);
				retourCGIPOST(filename, br, dos);
			}
			
		}
		
	} // traiterRequete    
	
	private static void retourFichier(String f,DataOutputStream dos) throws IOException {    
		/*	- Si le fichier existe, on prepare les infos status, contentType, contentLineLength qui conviennent on les envoit, et on envoit le fichier (methode envoiFichier)      
		 * - Si le fichier n'existe pas on prepare les infos qui conviennent         
		 * et on les envoit          */  
		debug("retourFichier entered",2);
		File file = null;
		try {
			debug("Try entered",2);
			file = new File(f);
		}catch (Exception e) {
			// TODO: handle exception
			debug("File not found on server", 2);
		}
		debug("First file passed",2);
		
		if(file == null || !file.exists()) //404 error
		{
			statusLine = "HTTP/1.1 404 Not Found";
			f = "notFound.html";
			debug("Going into 404 mode",2);
			file = new File(f);
		}
		else
		{
			debug("File found",2);
			statusLine = "HTTP/1.1 200 OK";
		}
		contentTypeLine = "Content-Type: "+contentType(f);
		contentLengthLine = "Content-Length: " + file.length();
		FileInputStream fis = new FileInputStream(file);
		entete(dos);
		envoiFichier(fis, dos);
	} // retourFichier   
	
	private static void envoiFichier(InputStream fis, DataOutputStream os) throws IOException {  
		byte[] buffer = new byte[1024] ;       
		int bytes = 0 ;     
		while ((bytes = fis.read(buffer)) != -1 ) {     
			os.write(buffer, 0, bytes);      
		}      
		envoi("\r\n",os);   
	} // envoiFichier    
	
	private static String executer(String f) throws IOException {       
		String R = "";    
		/*    Lance l'execution de la commande "f", et lit toutes les lignes   
		 *  qui lui sont retournees par l'execution de cette commande. On   
		 *   lit ligne ‡ ligne jusqu'a avoir une valeur de chaine null.  
		 *     Toutes ces lignes sont accumulees dans une chaine qui   
		 *      est retournee en fin d'execution.     */ 
		if(f.split(" ", 0)[0].endsWith(".py"))
		{
			Process p = Runtime.getRuntime().exec("python "+f);
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = null;
			//PrintWriter out = new PrintWriter("tmp.html");
				
			while((line = in.readLine()) != null)
			{
				//out.print(line); 
				R = R+line;
			}
			//out.close();
		}
		else
		{
			debug("In executer (POST)",2);
			debug("Content:"+f,2);
			String argsLine = f.split(" ")[1];
			String [] args = argsLine.split("&");
			String name ="default";
			String age="45";
			
			for(String arg : args)
			{
				String[] pair = arg.split("=");
				if(pair[0].contentEquals("name"))
				{
					name=pair[1];
				}
				if(pair[0].contentEquals("age"))
				{
					age=pair[1];
				}
			}
			String begin="<DOCTYPE html>\n<html>\n<body>\n<p>Bienvenue ";
			String body = begin+name+" vous avez "+age+" ans";
			return body+"</p>\n</body>\n</html>";
		}
		
		return R;
	} // executer
	
	private static void retourCGIPOST(String f, BufferedReader br, DataOutputStream dos) throws IOException {   
		/*    On lit toutes les lignes jusqu'a trouver une ligne commencant par Content-Length  
		 *   Lorsque cette ligne est trouvee, on extrait le nombre qui suit(nombre
		 *   de caracteres a lire).    
		 *   On lit une ligne vide    
		 *   On lit les caracteres dont le nombre a ete trouve ci-dessus   
		 *   on les range dans une chaine,    
		 *   On appelle la methode 'executer' en lui donnant comme parametre  
		 *   une chaine qui est la concatenation du nom de fichier, d'un espace 
		 *   et de la chaine de parametres.  
		 *   'executer' retourne une chaine qui est la reponse ‡ renvoyer     
		 *   au client, apres avoir envoye les infos status, contentTypeLine, ....     */ 
		String line = "";
		int length = 0;
		do {
			line = lireLigne("Lecture des liges POST:", br);
			if(line.startsWith("Content-Length:"))
			{
				length = Integer.parseInt(line.split(":")[1].trim());
			}
			/*else if(line.isEmpty())
			{
				break;
			}*/
		}while(!line.isEmpty());
		
		debug("Lecture des caracteres du POST ("+length+" bytes)",2);
		if(length != 0)
		{
			char msg[] = new char[length];
			br.read(msg);
			String str = new String(msg);
			debug("Read line: "+str,2);
			String generated = executer(f+" "+str);
			debug("Generated: "+generated,2);
			
			contentTypeLine = "Content-Type: text/html";
			contentLengthLine = "Content-Length: " + generated.length();
			statusLine = "HTTP/1.1 200 OK";
			
			//retourFichier("tmp.html", dos); //ou:
			//FileInputStream fis = new FileInputStream("tmp.html");
			//envoiFichier(fis, dos);
			entete(dos);
			InputStream stream = new ByteArrayInputStream(generated.getBytes(StandardCharsets.UTF_8));
			envoiFichier(stream, dos);
		}
	}     
	
	private static void envoi(String m, DataOutputStream dos) throws IOException {       
		dos.write(m.getBytes());    
	} //envoi    
	
	private static void entete(DataOutputStream dos) throws IOException {      
		envoi(statusLine+"\r\n",dos);      
		envoi(serverLine+"\r\n",dos);     
		envoi(contentTypeLine+"\r\n",dos);   
		envoi(contentLengthLine+"\r\n",dos);    
		envoi("\r\n",dos);    
	} // entete    
	
	private static String contentType(String fileName) {       
		if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {          
			return "text/html";      
		}       
		return "";   
	} // contentType     
	
	public static void main (String args []) throws IOException {       
		go (1234);       
		System.out.println("ARRET DU SERVEUR");        
	}
}
