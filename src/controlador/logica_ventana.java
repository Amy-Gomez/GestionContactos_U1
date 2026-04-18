package controlador;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import vista.ventana;
import modelo.*;

//Definición de la clase logica_ventana que implementa tres interfaces para manejar eventos.
public class logica_ventana implements ActionListener, ListSelectionListener, ItemListener {
	private ventana delegado; // Referencia a la ventana principal que contiene la GUI.
	private String nombres, email, telefono, categoria=""; // Variables para almacenar datos del contacto.
	private persona persona; // Objeto de tipo persona, que representa un contacto.
	private List<persona> contactos; // Lista de objetos persona que representa todos los contactos.
	private boolean favorito = false; // Booleano que indica si un contacto es favorito.
	private TableRowSorter<DefaultTableModel> sorter;

	// Constructor que inicializa la clase y configura los escuchadores de eventos para los componentes de la GUI.
	public logica_ventana(ventana delegado) {
		  // Asigna la ventana recibida como parámetro a la variable de instancia delegado.
	    this.delegado = delegado;
	    // Carga los contactos almacenados al inicializar.
	    cargarContactosRegistrados(); 
	    // Registra los ActionListener para los botones de la GUI.
	    this.delegado.btn_add.addActionListener(this);
	    this.delegado.btn_eliminar.addActionListener(this);
	    this.delegado.btn_modificar.addActionListener(this);
	    // Registra los ListSelectionListener para la lista de contactos.
	    this.delegado.lst_contactos.addListSelectionListener(this);
	    // Registra los ItemListener para el JComboBox de categoría y el JCheckBox de favoritos.
	    this.delegado.cmb_categoria.addItemListener(this);
	    this.delegado.chb_favorito.addItemListener(this);

	    this.delegado.tbl_contactos.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
	    	@Override
	    	public void valueChanged(ListSelectionEvent e) {
	    		if (!e.getValueIsAdjusting()) {
	    			int row = delegado.tbl_contactos.getSelectedRow();
	    			if (row != -1) {
	    				int modelRow = delegado.tbl_contactos.convertRowIndexToModel(row);
	    				cargarContacto(modelRow + 1);
	    			}
	    		}
	    	}
	    });

	    this.delegado.txt_nombres.addKeyListener(new KeyAdapter() {
	    	@Override
	    	public void keyPressed(KeyEvent e) {
	    		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
	    			delegado.btn_add.doClick();
	    		}
	    	}
	    });

	    this.delegado.tbl_contactos.addKeyListener(new KeyAdapter() {
	    	@Override
	    	public void keyPressed(KeyEvent e) {
	    		if (e.getKeyCode() == KeyEvent.VK_DELETE) {
	    			eliminarContacto();
	    		} else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_E) {
	    			exportarCSV();
	    		}
	    	}
	    });

	    JPopupMenu menuContextual = new JPopupMenu();
	    JMenuItem menuEliminar = new JMenuItem("Eliminar");
	    JMenuItem menuModificar = new JMenuItem("Modificar");
	    JMenuItem menuExportar = new JMenuItem("Exportar CSV");

	    menuEliminar.addActionListener(new ActionListener() {
	    	@Override
	    	public void actionPerformed(ActionEvent e) { eliminarContacto(); }
	    });
	    menuModificar.addActionListener(new ActionListener() {
	    	@Override
	    	public void actionPerformed(ActionEvent e) { modificarContacto(); }
	    });
	    menuExportar.addActionListener(new ActionListener() {
	    	@Override
	    	public void actionPerformed(ActionEvent e) { exportarCSV(); }
	    });

	    menuContextual.add(menuEliminar);
	    menuContextual.add(menuModificar);
	    menuContextual.add(menuExportar);

	    this.delegado.tbl_contactos.addMouseListener(new MouseAdapter() {
	    	@Override
	    	public void mousePressed(MouseEvent e) {
	    		if (e.isPopupTrigger()) { mostrarMenu(e, menuContextual); }
	    	}
	    	@Override
	    	public void mouseReleased(MouseEvent e) {
	    		if (e.isPopupTrigger()) { mostrarMenu(e, menuContextual); }
	    	}
	    	private void mostrarMenu(MouseEvent e, JPopupMenu menu) {
	    		int row = delegado.tbl_contactos.rowAtPoint(e.getPoint());
	    		if (row >= 0) {
	    			delegado.tbl_contactos.setRowSelectionInterval(row, row);
	    		}
	    		menu.show(e.getComponent(), e.getX(), e.getY());
	    	}
	    });

	    this.delegado.txt_buscar.getDocument().addDocumentListener(new DocumentListener() {
	    	@Override
	    	public void insertUpdate(DocumentEvent e) { filtrar(); }
	    	@Override
	    	public void removeUpdate(DocumentEvent e) { filtrar(); }
	    	@Override
	    	public void changedUpdate(DocumentEvent e) { filtrar(); }
	    });
	}

	private void filtrar() {
		String texto = delegado.txt_buscar.getText();
		if (sorter != null) {
			if (texto.trim().isEmpty()) {
				sorter.setRowFilter(null);
			} else {
				sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto, 0));
			}
		}
	}

	// Método privado para inicializar las variables con los valores ingresados en la GUI.
	private void incializacionCampos() {
		// Obtiene el texto ingresado en los campos de nombres, email y teléfono de la GUI.
		nombres = delegado.txt_nombres.getText();
		email = delegado.txt_email.getText();
		telefono = delegado.txt_telefono.getText();
	}

	// Método privado para cargar los contactos almacenados desde un archivo.
	private void cargarContactosRegistrados() {
		delegado.progressBar.setValue(0);
		delegado.progressBar.setString("Cargando...");
		 try {
		        // Lee los contactos almacenados utilizando una instancia de personaDAO.
		        contactos = new personaDAO(new persona()).leerArchivo();
		        DefaultListModel modelo = new DefaultListModel();
		        DefaultTableModel modeloTabla = (DefaultTableModel) delegado.tbl_contactos.getModel();
		        modeloTabla.setRowCount(0);
		        delegado.progressBar.setMaximum(contactos.size());
		        // Agrega cada contacto al modelo de la lista de contactos de la GUI.
		        for (int i = 0; i < contactos.size(); i++) {
		        	persona contacto = contactos.get(i);
		            modelo.addElement(contacto.formatoLista());
		            if (i > 0) {
		            	modeloTabla.addRow(new Object[]{
		            		contacto.getNombre(),
		            		contacto.getTelefono(),
		            		contacto.getEmail(),
		            		contacto.getCategoria(),
		            		contacto.isFavorito()
		            	});
		            }
		            delegado.progressBar.setValue(i + 1);
		        }
		        // Establece el modelo actualizado en la lista de contactos de la GUI.
		        delegado.lst_contactos.setModel(modelo);
		        sorter = new TableRowSorter<>(modeloTabla);
		        delegado.tbl_contactos.setRowSorter(sorter);
		        delegado.progressBar.setString("Listo");
		        actualizarEstadisticas();
		    } catch (IOException e) {
		        // Muestra un mensaje de error si ocurre una excepción al cargar los contactos.
		        JOptionPane.showMessageDialog(delegado, "Existen problemas al cargar todos los contactos");
		        delegado.progressBar.setString("Error");
		    }
	}

	private void actualizarEstadisticas() {
		int total = contactos.size() - 1;
		if (total < 0) total = 0;
		int familia = 0, amigos = 0, trabajo = 0, favoritos = 0;
		for (int i = 1; i < contactos.size(); i++) {
			persona p = contactos.get(i);
			if ("Familia".equalsIgnoreCase(p.getCategoria())) familia++;
			else if ("Amigos".equalsIgnoreCase(p.getCategoria())) amigos++;
			else if ("Trabajo".equalsIgnoreCase(p.getCategoria())) trabajo++;
			if (p.isFavorito()) favoritos++;
		}
		String html = "<html><body style='font-family:Tahoma;padding:15px'>"
			+ "<h2>Resumen de contactos</h2>"
			+ "<p>Total: <b>" + total + "</b></p>"
			+ "<p>Familia: <b>" + familia + "</b></p>"
			+ "<p>Amigos: <b>" + amigos + "</b></p>"
			+ "<p>Trabajo: <b>" + trabajo + "</b></p>"
			+ "<p>Favoritos: <b>" + favoritos + "</b></p>"
			+ "</body></html>";
		delegado.lbl_estadisticas.setText(html);
	}

	private void eliminarContacto() {
		int row = delegado.tbl_contactos.getSelectedRow();
		if (row != -1) {
			int modelRow = delegado.tbl_contactos.convertRowIndexToModel(row);
			int realIndex = modelRow + 1;
			if (realIndex < contactos.size()) {
				int confirm = JOptionPane.showConfirmDialog(delegado,
					"¿Desea eliminar el contacto seleccionado?", "Confirmar eliminación",
					JOptionPane.YES_NO_OPTION);
				if (confirm == JOptionPane.YES_OPTION) {
					contactos.remove(realIndex);
					try {
						new personaDAO(new persona()).actualizarContactos(contactos.subList(1, contactos.size()));
					} catch (IOException ex) {
						JOptionPane.showMessageDialog(delegado, "Error al eliminar el contacto");
					}
					limpiarCampos();
				}
			}
		}
	}

	private void modificarContacto() {
		int row = delegado.tbl_contactos.getSelectedRow();
		if (row != -1) {
			int modelRow = delegado.tbl_contactos.convertRowIndexToModel(row);
			int realIndex = modelRow + 1;
			if (realIndex < contactos.size()) {
				incializacionCampos();
				if ((!nombres.equals("")) && (!telefono.equals("")) && (!email.equals(""))) {
					if ((!categoria.equals("Elija una Categoria")) && (!categoria.equals(""))) {
						contactos.get(realIndex).setNombre(nombres);
						contactos.get(realIndex).setTelefono(telefono);
						contactos.get(realIndex).setEmail(email);
						contactos.get(realIndex).setCategoria(categoria);
						contactos.get(realIndex).setFavorito(favorito);
						try {
							new personaDAO(new persona()).actualizarContactos(contactos.subList(1, contactos.size()));
						} catch (IOException ex) {
							JOptionPane.showMessageDialog(delegado, "Error al modificar el contacto");
						}
						limpiarCampos();
						JOptionPane.showMessageDialog(delegado, "Contacto Modificado!!!");
					} else {
						JOptionPane.showMessageDialog(delegado, "Elija una Categoria!!!");
					}
				} else {
					JOptionPane.showMessageDialog(delegado, "Todos los campos deben ser llenados!!!");
				}
			}
		}
	}

	private void exportarCSV() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Exportar contactos a CSV");
		if (chooser.showSaveDialog(delegado) == JFileChooser.APPROVE_OPTION) {
			try {
				new personaDAO(new persona()).exportarCSV(
					chooser.getSelectedFile().getAbsolutePath() + ".csv");
				JOptionPane.showMessageDialog(delegado, "Exportación exitosa!!!");
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(delegado, "Error al exportar los contactos");
			}
		}
	}

	// Método privado para limpiar los campos de entrada en la GUI y reiniciar variables.
	private void limpiarCampos() {
		// Limpia los campos de nombres, email y teléfono en la GUI.
	    delegado.txt_nombres.setText("");
	    delegado.txt_telefono.setText("");
	    delegado.txt_email.setText("");
	    // Reinicia las variables de categoría y favorito.
	    categoria = "";
	    favorito = false;
	    // Desmarca la casilla de favorito y establece la categoría por defecto.
	    delegado.chb_favorito.setSelected(favorito);
	    delegado.cmb_categoria.setSelectedIndex(0);
	    // Reinicia las variables con los valores actuales de la GUI.
	    incializacionCampos();
	    // Recarga los contactos en la lista de contactos de la GUI.
	    cargarContactosRegistrados();
	}

	// Método que maneja los eventos de acción (clic) en los botones.
	@Override
	public void actionPerformed(ActionEvent e) {
		incializacionCampos(); // Inicializa las variables con los valores actuales de la GUI.

	    // Verifica si el evento proviene del botón "Agregar".
	    if (e.getSource() == delegado.btn_add) {
	        // Verifica si los campos de nombres, teléfono y email no están vacíos.
	        if ((!nombres.equals("")) && (!telefono.equals("")) && (!email.equals(""))) {
	            // Verifica si se ha seleccionado una categoría válida.
	            if ((!categoria.equals("Elija una Categoria")) && (!categoria.equals(""))) {
	                // Crea un nuevo objeto persona con los datos ingresados y lo guarda.
	                persona = new persona(nombres, telefono, email, categoria, favorito);
	                new personaDAO(persona).escribirArchivo();
	                // Limpia los campos después de agregar el contacto.
	                limpiarCampos();
	                // Muestra un mensaje de éxito.
	                JOptionPane.showMessageDialog(delegado, "Contacto Registrado!!!");
	            } else {
	                // Muestra un mensaje de advertencia si no se ha seleccionado una categoría válida.
	                JOptionPane.showMessageDialog(delegado, "Elija una Categoria!!!");
	            }
	        } else {
	            // Muestra un mensaje de advertencia si algún campo está vacío.
	            JOptionPane.showMessageDialog(delegado, "Todos los campos deben ser llenados!!!");
	        }
	    } else if (e.getSource() == delegado.btn_eliminar) {
	        // Lugar para implementar la funcionalidad de eliminar un contacto.
	    	eliminarContacto();
	    } else if (e.getSource() == delegado.btn_modificar) {
	        // Lugar para implementar la funcionalidad de modificar un contacto.
	    	modificarContacto();
	    }
	}

	// Método que maneja los eventos de selección en la lista de contactos.
	@Override
	public void valueChanged(ListSelectionEvent e) {
		// Obtiene el índice del elemento seleccionado en la lista de contactos.
	    int index = delegado.lst_contactos.getSelectedIndex();
	    // Verifica si se ha seleccionado un índice válido en la lista.
	    if (index != -1) {
	        // Si el índice es mayor que cero (no se seleccionó la primera fila),
	        // carga los detalles del contacto seleccionado.
	        if (index > 0) {
	            cargarContacto(index);
	        }
	    } 
	}

	// Método privado para cargar los datos del contacto seleccionado en los campos de la GUI.
	private void cargarContacto(int index) {
		if (index < 0 || index >= contactos.size()) return;
		// Establece el nombre del contacto en el campo de texto de nombres.
	    delegado.txt_nombres.setText(contactos.get(index).getNombre());
	    // Establece el teléfono del contacto en el campo de texto de teléfono.
	    delegado.txt_telefono.setText(contactos.get(index).getTelefono());
	    // Establece el correo electrónico del contacto en el campo de texto de correo electrónico.
	    delegado.txt_email.setText(contactos.get(index).getEmail());
	    // Establece el estado de favorito del contacto en el JCheckBox de favorito.
	    delegado.chb_favorito.setSelected(contactos.get(index).isFavorito());
	    // Establece la categoría del contacto en el JComboBox de categoría.
	    delegado.cmb_categoria.setSelectedItem(contactos.get(index).getCategoria());
	}

	// Método que maneja los eventos de cambio de estado en los componentes cmb_categoria y chb_favorito.
	@Override
	public void itemStateChanged(ItemEvent e) {
		// Verifica si el evento proviene del JComboBox de categoría.
	    if (e.getSource() == delegado.cmb_categoria) {
	        // Obtiene el elemento seleccionado en el JComboBox y lo convierte en una cadena.
	        categoria = delegado.cmb_categoria.getSelectedItem().toString();
	        // Actualiza la categoría seleccionada en la variable "categoria".
	    } else if (e.getSource() == delegado.chb_favorito) {
	        // Verifica si el evento proviene del JCheckBox de favorito.
	        favorito = delegado.chb_favorito.isSelected();
	        // Obtiene el estado seleccionado del JCheckBox y actualiza el estado de favorito en la variable "favorito".
	    }
	}
}