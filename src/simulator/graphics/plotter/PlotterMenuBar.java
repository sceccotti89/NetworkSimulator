/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

		add( file );
		file.addSeparator();
		
		// Edit section.
		JMenu edit = new JMenu( "Edit" );
		showFPS = new JCheckBoxMenuItem( "Show FPS" );
        edit.add( showFPS );
        showFPS.setName( "1" );
        showFPS.addActionListener( this );
        
        showGrid = new JCheckBoxMenuItem( "Show Grid" );
        edit.add( showGrid );
        showGrid.setName( "2" );
        showGrid.addActionListener( this );
        
        updateSelectedValue();
        
        JMenuItem addPlot = new JMenuItem( "Add plot" );
        edit.add( addPlot );
        addPlot.setName( "3" );
        addPlot.addActionListener( this );
        
        JMenuItem makePlot = new JMenuItem( "Make plot" );
        edit.add( makePlot );
        makePlot.setName( "4" );
        makePlot.addActionListener( this );
        
        JMenu theme = new JMenu( "Theme" );
        ButtonGroup group = new ButtonGroup();
        edit.add( theme );
        theme.setName( "5" );
        theme.addActionListener( this );
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

			case( 1 ): // Show FPS
			    plotter.showFPS( item.isSelected() );
				break;
			
			case( 2 ): // Show grid
			    plotter.showGrid( item.isSelected() );
                break;
            
			case( 3 ): // Add plot.
			    f = new JFileChooser( Utils.RESULTS_DIR );
			    if (f.showOpenDialog( plotter.getFrame() ) == JFileChooser.APPROVE_OPTION) {
			        String dir  = f.getCurrentDirectory().getName();
			        String file = f.getSelectedFile().getName();
			        int index = file.lastIndexOf( '.' );
			        try {
                        plotter.addPlot( dir + "/" + file, null, file.substring( 0, index ) );
                    } catch ( IOException e1 ) {
                        e1.printStackTrace();
                    }
			    }
			    break;
		    
			case( 4 ): // Make plot.
			    // Open a dialog to edit the plotter settings.
                JDialog makePlot = new MakePlotDialog( plotter.getFrame(), plotter );
                makePlot.setVisible( true );
			    break;
			case( 5 ): // Theme
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
			    JDialog settings = new SettingsDialog( plotter.getFrame(), plotter.getsettings() );
			    settings.setVisible( true );
			    break;
		}
	}
}