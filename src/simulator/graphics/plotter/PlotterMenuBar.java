/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

import simulator.graphics.plotter.Plotter.Theme;
import simulator.utils.Utils;

public class PlotterMenuBar extends JMenuBar implements ActionListener
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = 8213088039082560260L;
    
    private final JCheckBoxMenuItem showFPS;
    private final JCheckBoxMenuItem showGrid;
    
    private final Plotter plotter;

    public PlotterMenuBar( final Plotter plotter )
    {
        this.plotter = plotter;
        
        JMenu file = new JMenu( "File" );
        JMenuItem new_project = new JMenuItem( "Save as image.." );
        file.add( new_project );
        new_project.setName( "0" );
        new_project.addActionListener( this );
        
        JMenuItem new_file = new JMenuItem( "Save as file.." );
        file.add( new_file );
        new_file.setName( "1" );
        new_file.addActionListener( this );

        add( file );
        file.addSeparator();
        
        // Edit section.
        JMenu edit = new JMenu( "Edit" );
        showFPS = new JCheckBoxMenuItem( "Show FPS" );
        edit.add( showFPS );
        showFPS.setName( "2" );
        showFPS.addActionListener( this );
        
        showGrid = new JCheckBoxMenuItem( "Show Grid" );
        edit.add( showGrid );
        showGrid.setName( "3" );
        showGrid.addActionListener( this );
        
        updateSelectedValue();
        
        JMenuItem addPlot = new JMenuItem( "Add plot" );
        edit.add( addPlot );
        addPlot.setName( "4" );
        addPlot.addActionListener( this );
        
        JMenuItem makePlot = new JMenuItem( "Make plot" );
        edit.add( makePlot );
        makePlot.setName( "5" );
        makePlot.addActionListener( this );
        
        JMenu theme = new JMenu( "Theme" );
        ButtonGroup group = new ButtonGroup();
        edit.add( theme );
        theme.addSeparator();
        
        JRadioButtonMenuItem white = new JRadioButtonMenuItem( "White" );
        white.setSelected( plotter.getTheme() == Theme.WHITE );
        theme.add( white );
        white.setName( "6" );
        white.addActionListener( this );
        group.add( white );
        
        JRadioButtonMenuItem black = new JRadioButtonMenuItem( "Black" );
        black.setSelected( plotter.getTheme() == Theme.BLACK );
        theme.add( black );
        black.setName( "7" );
        black.addActionListener( this );
        group.add( black );
        
        JMenuItem settings = new JMenuItem( "Settings" );
        edit.add( settings );
        settings.setName( "8" );
        settings.addActionListener( this );
        
        // TODO Complete...
        
        
        add( edit );
    }
    
    public void updateSelectedValue() {
        showFPS.setSelected( plotter.isShowingFPS() );
        showGrid.setSelected( plotter.isShowingGrid() );
    }

    @Override
    public void actionPerformed( ActionEvent e )
    {
        JMenuItem item = (JMenuItem) e.getSource();
        final int ID = Integer.parseInt( item.getName() );
        switch( ID ){
            case( 0 ): // Save as image.
                JFileChooser f = new JFileChooser( Utils.IMAGES_DIR ) {
                    /** Generated Serial ID */
                    private static final long serialVersionUID = 6328845630088756577L;
                    
                    @Override
                    public void approveSelection()
                    {
                        String file = getCurrentDirectory().toString() + "/" + getSelectedFile().getName();
                        boolean save = false;
                        if (!Utils.existsFile( file )) {
                            save = true;
                        } else {
                            // File already exists: ask for override.
                            Object[] options = { "Yes", "No" };
                            int response = JOptionPane.showOptionDialog(
                                            this,
                                            "Image \"" + file + "\" already exists.\n" +
                                            "Do you want to override it?",
                                            "Warning",
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.WARNING_MESSAGE,
                                            null,
                                            options,
                                            options[1] );
                            save = (response == 0); // Answer YES.
                        }
                        
                        if (save) {
                            try {
                                plotter.createImage( getSelectedFile().getName() );
                            } catch ( IOException e1 ) {
                                e1.printStackTrace();
                            }
                        }
                        
                        super.approveSelection();
                    }
                };
                //FileNameExtensionFilter filter = new FileNameExtensionFilter( "Image Extensions", "jpg", "png", "gif" );
                //f.setFileFilter( filter );
                f.showSaveDialog( plotter.getFrame() );
                break;
            
            case( 1 ): // Save all the plots in a single file.
                f = new JFileChooser() {
                    /** Generated serial ID. */
                    private static final long serialVersionUID = 4540638898219035335L;

                    @Override
                    public void approveSelection()
                    {
                        File f = getSelectedFile();
                        if (f.exists() && getDialogType() == SAVE_DIALOG) {
                            int result = JOptionPane.showConfirmDialog( this, "The file exists, overwrite?",
                                                                              "Existing file", JOptionPane.YES_NO_OPTION );
                            switch (result) {
                                case JOptionPane.YES_OPTION:
                                    super.approveSelection();
                                    return;
                                case JOptionPane.NO_OPTION:
                                    return;
                                case JOptionPane.CLOSED_OPTION:
                                    return;
                            }
                        }
                        super.approveSelection();
                    }        
                };
            
                if (f.showSaveDialog( plotter.getFrame() ) == JFileChooser.APPROVE_OPTION) {
                    String dir  = f.getCurrentDirectory().getAbsolutePath();
                    String file = f.getSelectedFile().getName();
                    try {
                        plotter.savePlot( dir, file );
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                break;

            case( 2 ): // Show FPS.
                plotter.showFPS( item.isSelected() );
                break;
            
            case( 3 ): // Show grid.
                plotter.showGrid( item.isSelected() );
                break;
            
            case( 4 ): // Add plot.
                f = new JFileChooser( Utils.RESULTS_DIR );
                if (f.showOpenDialog( plotter.getFrame() ) == JFileChooser.APPROVE_OPTION) {
                    String dir  = f.getCurrentDirectory().getAbsolutePath();
                    String file = f.getSelectedFile().getName();
                    int index = file.lastIndexOf( '.' );
                    try {
                        plotter.addPlot( dir + "/" + file, null, file.substring( 0, index ) );
                    } catch ( IOException e1 ) {
                        e1.printStackTrace();
                    }
                }
                break;
            
            case( 5 ): // Make plot.
                // Open a dialog to edit the plotter settings.
                JDialog makePlot = new MakePlotDialog( plotter.getFrame(), plotter );
                makePlot.setVisible( true );
                break;
            case( 6 ): // Theme => White
                item.setSelected( true );
                plotter.setTheme( Theme.WHITE );
                break;
            case( 7 ): // Theme => Black
                item.setSelected( true );
                plotter.setTheme( Theme.BLACK );
                break;
            case( 8 ): // Settings.
                // Open a dialog to edit the plotter settings.
                JDialog settings = new SettingsDialog( plotter.getFrame(), plotter.getSettings() );
                settings.setVisible( true );
                break;
        }
    }
}
