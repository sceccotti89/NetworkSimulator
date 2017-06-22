package simulator.graphics.plotter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class MenuBar extends JMenuBar implements ActionListener
{
    /* serial ID */
    private static final long serialVersionUID = 8213088039082560260L;

    public MenuBar()
    {
        JMenu file = new JMenu( "File" );
        JMenuItem new_project = new JMenuItem( "New Project" );
        file.add( new_project );
        new_project.setName( "0" );
        new_project.addActionListener( this );
        JMenuItem open_project = new JMenuItem( "Open Project..." );
        file.add( open_project );
        open_project.setName( "1" );
        open_project.addActionListener( this );

        add( file );

        JMenu edit = new JMenu( "Edit" );
        // TODO completare
        add( edit );
    }

    @Override
    public void actionPerformed( ActionEvent e )
    {
        int ID = Integer.parseInt( ((JMenuItem) e.getSource()).getName() );
        switch( ID ){
            case( 0 ):
                break;

            case( 1 ):
                break;
        }
    }
}
