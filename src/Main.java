import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
 
 
public class Main {
	
	public static FileWriter r;
	public static String command,command_ingrediente,command_jonc;
	public static Map<String,Integer> listaIngrediente;	
	public static XMLDecoder in;
	public static XMLEncoder out;
	public static String barcodeLink,links,link_personalizat,barcode,price,weight,title,ingredients;
	
 
	public static void main(String[] args) throws SQLException, IOException 
	{		
			
			if(Files.exists(Paths.get(Paths.get(System.getProperty("user.dir")).toString()+"/ingrediente.xml")))
			{
				in= new XMLDecoder(new BufferedInputStream(new FileInputStream("ingrediente.xml")));
				listaIngrediente=(HashMap<String,Integer>)in.readObject();
			}
			else
			{
				
				listaIngrediente=new HashMap<String, Integer>();
			}
			
			r = new FileWriter(new File("produse.sql"));
			command="";
			processPage("http://www.carrefour-online.ro/carne_mezeluri_si_lactate/lactate_si_oua/",0);
	}
	
	public static String getBarcodeAndLink(Document doc1)
	{
		String returnText="";
		Elements barcode= doc1.select("ul.gallery-wrapper li img");
		for(Element aux : barcode)
		{
			if(aux.hasAttr("src"))
			{
				System.out.println("Poza:"+aux.attr("abs:src"));
				returnText+=aux.attr("abs:src");
				String aaa=aux.attr("abs:src");
				aaa=aaa.substring(aaa.indexOf("_")+1, aaa.lastIndexOf("."));
				System.out.println("Barcode:"+aaa);
				returnText+="\n";
				returnText+=aaa;
				break;
			
			}
		}
		return returnText;
	}
	
	public static String getPrice(Document doc1)
	{
		
		Elements price=doc1.select("div.price p span");
		String pret="";
		for(Element aux:price )
		{
			if(aux.hasText())
			{
				pret+=aux.text();
			}
		}
		
		System.out.println("Pret:"+pret);
		return pret;
	}
	
	public static String getWeight(Document doc1)
	{
		Elements weight=doc1.select("dl.single dt +dd");
		String greutate="";
		boolean gasit=false;
		for(Element e :weight)
		{
			if(gasit==false)
			{
				gasit=true;
				continue;
			}
			
			if(gasit==true)
			{
				greutate=e.text();
				break;
			}
			
		}
		
		System.out.println("Weight:"+greutate);
		return greutate;
	}
	
	public static String getTitlu(Document doc1)
	{
		String returnText="";
		Elements pageProduct =doc1.select("div.product>h3");
		for(Element aux:pageProduct)
		{
			System.out.println("Titlu:"+aux.text());
			returnText+=aux.text();
		}
		return returnText;
	}
	
	public static String getIngredients(Document doc1)
	{
		String returnText="";
		Elements ingredients=doc1.select(".single");
		for(Element aux:ingredients)
		{
			if(aux.text().contains("Ingrediente"))
				{
					System.out.println("Ingrediente:"+aux.text().substring(aux.text().indexOf("Ingrediente"), aux.text().length()-1));
					returnText+=aux.text().substring(aux.text().indexOf("Ingrediente"), aux.text().length()-1);
				
				}
		}
		
		return returnText;
	}
 
	public static void processPage(String URL,int pas) throws SQLException, IOException{
		
			//System.setProperty("http.proxyHost", "74.178.251.184");
			//System.setProperty("http.proxyPort", "8080");
			int pagina=1;
			
			while(true)
			{
				System.out.println("Pagina:"+pagina);
				Document doc=null;
				try
				{
					doc = Jsoup.connect("http://www.carrefour-online.ro/apa_alte_bauturi_si_alte_produse/"+"?pagina="+pagina).get();
					if(pagina==22)
					{
						Thread.sleep(5000);
						out = new XMLEncoder(new BufferedOutputStream(new FileOutputStream("ingrediente.xml")));
						out.writeObject(listaIngrediente);
						out.flush();
						Thread.sleep(2000);
						in.close();
						out.close();
						r.flush();
						r.close();
						System.exit(0);	
					}
				}
				catch(Exception e)
				{
					System.exit(0);				
				}
				
				Elements questions = doc.select(".name a");				
				long xxx=1;
				for(Element link: questions){					
					if(xxx%2==1)
					{	
						if(link.hasAttr("title"))
							System.out.println(link.attr("title"));						
						if(link.hasAttr("href"))
						{	
							System.out.println(link.attr("abs:href"));
							Document doc1=null;
							try{								
								doc1=Jsoup.connect(link.attr("abs:href")).timeout(0).get();
								///Extract barcode and image link
								command="INSERT INTO Product(barcode,name,weight,price,image,keywords,category_id) VALUES(";
								command_ingrediente="INSERT INTO Ingredient VALUES(";
								
								 barcodeLink=getBarcodeAndLink(doc1);	
								 links=barcodeLink.substring(0, barcodeLink.indexOf("\n"));
								 link_personalizat=links.substring(0, links.indexOf("?"));
								link_personalizat+="?w=460&h=460&q=100&bc=transparent";
								 barcode=barcodeLink.substring(barcodeLink.indexOf("\n"), barcodeLink.length()).replace("\n", "");
								///Extract price								
								 price=getPrice(doc1);								
								////Extract weight								
								 weight=getWeight(doc1);								
								///EXtract title								
								 title=getTitlu(doc1);	
								 if(title.contains("'"))
								 {
									 title=title.replace("'", "'||chr(39)||'");
								 }
								//Extract ingrediesns								
								ingredients=getIngredients(doc1);
								
								if(ingredients.contains(">M"))
								{
									ingredients=ingredients.substring(0, ingredients.indexOf(">M"));
								}
								
								if(title.toLowerCase().contains("apa"))
									ingredients="apa";
								else
								{
								
									if(ingredients.contains(".0")||ingredients.contains(".1")||ingredients.contains(".2")||ingredients.contains(".3")||ingredients.contains(".4")||ingredients.contains(".5")||ingredients.contains(".6")||ingredients.contains(".7")||ingredients.contains(".8")||ingredients.contains(".9"))
									{
										char sir_aux[]=ingredients.toCharArray();
										for(int i=1;i<ingredients.length();i++)
										{
											if(sir_aux[i-1]=='.'&& Character.isDigit(sir_aux[i]))
											{
												sir_aux[i-1]='$';
											}
										}
										ingredients=String.valueOf(sir_aux);
									}
									
									if(ingredients.contains(".")|| ingredients.endsWith("."))
										ingredients=ingredients.substring(12, ingredients.indexOf('.'));
									else
										ingredients=ingredients.substring(12, ingredients.length());
									
									
									char sir_aux[]=ingredients.toCharArray();
									for(int i=1;i<ingredients.length();i++)
									{
										if(sir_aux[i-1]=='$'&& Character.isDigit(sir_aux[i]))
										{
											sir_aux[i-1]='.';
										}
									}
									ingredients=String.valueOf(sir_aux);
								}
								String aux="";
								
								boolean gasit=false;
								for(int i=0;i<ingredients.length();i++)
								{	
									if(gasit==false)
									{
										if(ingredients.charAt(i)=='(')
											gasit=true;
										else
											aux+=ingredients.charAt(i);
											
									}
									
									else 
									{
										if(ingredients.charAt(i)==')')
											gasit=false;
									}
									
									
								}
								
								
								String ingred[]=aux.split(",");
								for(int i=0;i<ingred.length;i++)
								{
									ingred[i]=ingred[i].trim();
									if(!listaIngrediente.containsKey(ingred[i]))
									{
											listaIngrediente.put(ingred[i], listaIngrediente.size());
											System.out.println("Inserez:"+ingred[i]);
											command_ingrediente="INSERT INTO Ingredient VALUES(";
											command_ingrediente+="'"+listaIngrediente.get(ingred[i])+"',";
											command_ingrediente+="'"+ingred[i]+"')";
											System.out.println("COmanda de insert in ingrediente este:"+command_ingrediente);
											r.write(command_ingrediente+"\n");
									}
										
								}
								
								
								for(int i=0;i<ingred.length;i++)
								{
									
									command_jonc="INSERT INTO Product-Ingredient VALUES(";
									command_jonc+="'"+listaIngrediente.get(ingred[i])+"',";
									command_jonc+="'"+barcode+"')";
									System.out.println("Comm pt jonc este:"+command_jonc);
									r.write(command_jonc+"\n");
								}
								
								System.out.println();
								command+="'"+barcode+"',";
								command+="'"+title+"',";
								command+="'"+weight+"',";
								command+="'"+price+"',";
								command+="'"+link_personalizat+"',";
								command+="'"+barcode+" "+title+"',"+3+")";
								System.out.println("Comanda de insert:"+command);
								r.write(command+"\n");
								command="";
								r.flush();
								
								
							}
							catch(Exception e)
							{
								/*
								ingredients=ingredients.substring(12, ingredients.length()-1);
								String aux="";
								boolean gasit=false;
								for(int i=0;i<ingredients.length();i++)
								{	
									if(gasit==false)
									{
										if(ingredients.charAt(i)=='(')
											gasit=true;
										else
											aux+=ingredients.charAt(i);
											
									}
									
									else 
									{
										if(ingredients.charAt(i)==')')
											gasit=false;
									}
									
									
								}
								String ingred[]=aux.split(",");
								for(int i=0;i<ingred.length;i++)
								{
									ingred[i]=ingred[i].trim();
								}
								System.out.print("Ingrediente pe pasi:");
								for(int i=0;i<ingred.length;i++)
								{
									System.out.print(ingred[i]+" ");
								}
								System.out.println();
								command+="'"+barcode+"',";
								command+="'"+title+"',";
								command+="'"+weight+"',";
								command+="'"+price+"',";
								command+="'"+link_personalizat+"',";
								command+="'"+barcode+" "+title+"',"+1+")";
								System.out.println("Comanda de insert:"+command);
								r.write(command+"\n");
								command="";
								*/
								
								System.out.println(e.getMessage());
							}						
						}					
					}
					xxx++;
					System.out.println();
					
				}
				pagina++;
			}
		
	}
}