package modelo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//Definición de la clase pública "personaDAO"
public class personaDAO {
	
	// Declaración de atributos privados de la clase "personaDAO"
	private File archivo; // Archivo donde se almacenarán los datos de los contactos
	private persona persona; // Objeto "persona" que se gestionará
	
	// Constructor público de la clase "personaDAO" que recibe un objeto "persona" como parámetro
	public personaDAO(persona persona) {
		this.persona = persona; // Asigna el objeto "persona" recibido al atributo de la clase
		archivo = new File("c:/gestionContactos"); // Establece la ruta donde se alojará el archivo
		// Llama al método para preparar el archivo
		prepararArchivo();
	}
	
	// Método privado para gestionar el archivo utilizando la clase File
	private void prepararArchivo() {
		// Verifica si el directorio existe
		if (!archivo.exists()) { // Si el directorio no existe, se crea
			archivo.mkdir();
		}
		
		// Accede al archivo "datosContactos.csv" dentro del directorio especificado
		archivo = new File(archivo.getAbsolutePath(), "datosContactos.csv");
		// Verifica si el archivo existe
		if (!archivo.exists()) { // Si el archivo no existe, se crea
			try {
				archivo.createNewFile();
				//Prepara el encabezado para el archivo de csv
				String encabezado=String.format("%s;%s;%s;%s;%s", "NOMBRE", "TELEFONO", "EMAIL", "CATEGORIA","FAVORITO");
//				persona.datosContacto(encabezado);
				escribir(encabezado);
			} catch (IOException e) {
				// Maneja la excepción de entrada/salida
				e.printStackTrace();
			}
		}
	}
	private void escribir(String texto){
		// Prepara el archivo para escribir en la última línea
		FileWriter escribir;
		try {
			escribir = new FileWriter(archivo.getAbsolutePath(), true);
			escribir.write(texto + "\n"); // Escribe los datos del contacto en el archivo
			// Cierra el archivo
			escribir.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	// Método público para escribir en el archivo
	public boolean escribirArchivo() {
//		// Prepara el archivo para escribir en la última línea
//		FileWriter escribir = new FileWriter(archivo.getAbsolutePath(), true);
//		escribir.write(persona.datosContacto() + "\n"); // Escribe los datos del contacto en el archivo
//		// Cierra el archivo
//		escribir.close();
		escribir(persona.datosContacto());
		return true; // Retorna true si la escritura fue exitosa
	}
	
	// Método público para leer los datos del archivo
	public List<persona> leerArchivo() throws IOException {
		// Cadena que contendrá toda la data del archivo
		String contactos = "";
		// Abre el archivo para leer
		FileReader leer = new FileReader(archivo.getAbsolutePath());
		int c;
		while ((c = leer.read()) != -1) { // Lee hasta la última línea del archivo
			contactos += String.valueOf((char) c);
		}
		// Separa cada contacto por salto de línea
		String[] datos = contactos.split("\n");
		// Crea una lista que almacenará cada persona encontrada
		List<persona> personas = new ArrayList<>();
		// Recorre cada contacto
		for (String contacto : datos) {
			// Crea una instancia de persona
			persona p = new persona();
			p.setNombre(contacto.split(";")[0]); // Asigna el nombre
			p.setTelefono(contacto.split(";")[1]); // Asigna el teléfono
			p.setEmail(contacto.split(";")[2]); // Asigna el email
			p.setCategoria(contacto.split(";")[3]); // Asigna la categoría
			p.setFavorito(Boolean.parseBoolean(contacto.split(";")[4])); // Asigna si es favorito
			// Añade cada persona a la lista
			personas.add(p);
		}
		// Cierra el archivo
		leer.close();
		// Retorna la lista de personas
		return personas;
	}
	
	// Método público para guardar los contactos modificados o eliminados
	public void actualizarContactos(List<persona> personas) throws IOException {
		// Borra los datos del archivo
		archivo.delete();
		// Recorre los elementos de la lista
		for (persona p : personas) {
			// Instancia el DAO
			new personaDAO(p);
			// Escribe en el archivo
			escribirArchivo();
		}
	}
}
