
package de.medint.rtpsharer.test.rtpapplet;

import java.applet.Applet;
import javax.media.rtp.*;
import javax.media.rtp.rtcp.*;
import javax.media.rtp.event.*;
import com.sun.media.rtp.RTPSessionMgr;
import java.awt.*;
import java.util.Vector;
import java.net.*;
import java.awt.event.*;
import java.lang.String;
import javax.media.*;
import javax.media.protocol.*;
import java.io.IOException;




public class RTPPlayerApplet  extends Applet implements ControllerListener, ReceiveStreamListener, ActionListener{
  
    InetAddress destaddr;
    String address;
    String portstr;
    String media;
    Player videoplayer = null;
    SessionManager videomgr = null;
    SessionManager audiomgr = null;
    Component visualComponent = null;
    Component controlComponent = null;
    Panel panel = null;
    Button audiobutton = null;
    Button videobutton = null;
    GridBagLayout gridbag = null;
    GridBagConstraints c = null;
        int width = 320;
    int height =0;
    Vector playerlist = new Vector();
   
    
    public void init(){
    	
        setLayout( new BorderLayout() );
        
        Panel buttonPanel = new Panel();
        
        buttonPanel.setLayout( new FlowLayout() );
        
        add("North", buttonPanel);
        
        media = getParameter("video");
        
        if (media.equals("On")){
            address = getParameter("videosession");
            portstr = getParameter("videoport");
            StartSessionManager(address,
                                StrToInt(portstr),
                                "video");
            
            if (videomgr == null){
                System.err.println("null video manager ");
                return;
            }
            
        }
        
    }

    /**
     * Start
     */
    public void start(){
         if (videoplayer != null){
            videoplayer.start();
        }
        
        
    }
   
    /**
     * Stop
     */
    public void stop(){
        if (videoplayer != null){
            videoplayer.close();
        }
      
    }

    // applet has been destroyed by the browser. Close the Session
    // Manager. 
    public void destroy(){
        // close the video and audio RTP SessionManagers
        String reason = "Shutdown RTP Player";
        
        if (videomgr != null){
            videomgr.closeSession(reason);
            videoplayer = null;
            videomgr = null;
        }
        
       
        super.destroy();
    }
            
   
    public void actionPerformed(ActionEvent event){
        Button button = (Button)event.getSource();
       
            
    }
    
    public String getAddress(){
        return  address;
    }
    
    public int getPort(){
        // the port has to be returned as an integer
        return StrToInt(portstr);
    }
    
    public String getMedia(){
        return media;
    }
    
    private int StrToInt(String str){
        if (str == null)
            return -1;
        Integer retint = new Integer(str);
        return  retint.intValue();
    }

    public synchronized void controllerUpdate(ControllerEvent event) {
        Player player = null;
        Controller controller = (Controller)event.getSource();
        if (controller instanceof Player)
            player  =(Player)event.getSource();
        
        if (player == null)
            return;
        
        
        if (event instanceof RealizeCompleteEvent) {
            // add the video player's visual component to the applet
            if (( visualComponent =
                  player.getVisualComponent())!= null){
                width = visualComponent.getPreferredSize().width;
                height += visualComponent.getPreferredSize().height;
                if (panel == null) {
                    panel = new Panel();
                    repositionPanel(width, height);
                    panel.setLayout(new BorderLayout());
                }
                panel.add("Center", visualComponent);
                panel.validate();
            }
            // add the player's control component to the applet
            if (( controlComponent = 
                  player.getControlPanelComponent()) != null){
                height += controlComponent.getPreferredSize().height;
                if (panel == null) {
                    panel = new Panel();
                    panel.setLayout(new BorderLayout());
                }
                repositionPanel(width, height);
                panel.add("South", controlComponent);
                panel.validate();
            }
            
            if (panel != null){
                add("Center", panel);
                invalidate();
            }
        }

        if (event instanceof SizeChangeEvent) {
            if (panel != null){
                SizeChangeEvent sce = (SizeChangeEvent) event;
                int nooWidth = sce.getWidth();
                int nooHeight = sce.getHeight();
                
                // Add the height of the default control component
                if (controlComponent != null)
                    nooHeight += controlComponent.getPreferredSize().height;
                
                // Set the new panel bounds and redraw
                repositionPanel(nooWidth, nooHeight);
            }
        }
        validate();
    }

    /**
     * The video/control component panel needs to be repositioned to sit
     * in the middle of the applet window.
     */
    void repositionPanel(int width, int height) {
        panel.setBounds(0,
                        0,
                        width,
                        height);
        panel.validate();
    }

    public void update( ReceiveStreamEvent event){
        SessionManager source =(SessionManager)event.getSource();
        
        // create a new player if a new recvstream is detected
        if (event instanceof NewReceiveStreamEvent){
            try{
                ReceiveStream stream = ((NewReceiveStreamEvent)event).getReceiveStream();
                DataSource dsource = stream.getDataSource();
                videoplayer = Manager.createPlayer(dsource);
            }catch (Exception e){
          System.err.println("RTPPlayerApplet Exception " + e.getMessage());
          e.printStackTrace();
            }
            
           
        }
        
        if (event instanceof RemotePayloadChangeEvent){
            // we received a payload change event. If a player was not
            // created for this ReceiveStream, create a player. If the
            // player already exists, RTPSM and JMF have taken care of
            // switching the payloads and we dont do anything.
            // If this is the first video player add me as the
            // controllerlistener before starting the player, else
            // just create a new player window.
        }
        
    }// end of RTPSessionUpdate
        
    private SessionManager StartSessionManager(String destaddrstr,
                                                  int port,
                                                  String media){
        // this method create a new RTPSessionMgr and adds this applet
        // as a SessionListener, before calling initSession() and startSession()
        SessionManager mymgr = new RTPSessionMgr();
        if (media.equals("video"))
            videomgr = mymgr;
      
        if (mymgr == null)
            return null;
        mymgr.addReceiveStreamListener(this);
        //if (media.equals("audio"))
        //  EncodingUtil.Init((SessionManager)mymgr);
        
        // for initSession() we must generate a CNAME and fill in the
        // RTP Session address and port
        String cname = mymgr.generateCNAME();
        String username = "jmf-user";

        SessionAddress localaddr = new SessionAddress();
        
        try{
            destaddr = InetAddress.getByName(destaddrstr);
        }catch (UnknownHostException e){
            System.err.println("inetaddress " + e.getMessage());
            e.printStackTrace();
        }    
        SessionAddress sessaddr = new SessionAddress(destaddr,
                                                           port,
                                                           destaddr,
                                                           port+1);
        
        SourceDescription[] userdesclist = new SourceDescription[4];
        int i;
        for(i=0; i< userdesclist.length;i++){
            if (i == 0){
                userdesclist[i] = new
                    SourceDescription(SourceDescription.SOURCE_DESC_EMAIL,
                                    "jmf-user@sun.com",
                                    1,
                                    false);
                continue;
            }

            if (i == 1){
                userdesclist[i] = new
              SourceDescription(SourceDescription.SOURCE_DESC_NAME,
                                    username,
                                    1,
                                    false);
                continue;
            }
            if ( i == 2){
                userdesclist[i] = new 
                    SourceDescription(SourceDescription.SOURCE_DESC_CNAME,
                                          cname,
                                      1,
                                      false);
                continue;
            }
            if (i == 3){ 
                userdesclist[i] = new
            SourceDescription(SourceDescription.SOURCE_DESC_TOOL,
                                  "JMF RTP Player v2.0",
                                  1,
                                  false);
                continue;
            }
        }// end of for
        
        // call initSession() and startSession() of the RTPsessionManager
        try{
            mymgr.initSession(localaddr,
                              mymgr.generateSSRC(),
                              userdesclist,
                              0.05,
                              0.25);
            mymgr.startSession(sessaddr,1,null);
        }catch (SessionManagerException e){
          System.err.println("RTPPlayerApplet: RTPSM Exception " + e.getMessage());
          e.printStackTrace();
          return null;
        }catch (IOException e){
           System.err.println("RTPPlayerApplet: IO Exception " + e.getMessage());
           e.printStackTrace();
           return null;
        }
        
        return mymgr;
    }       

}// end of class
