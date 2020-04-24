package com.downloader.main;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultCaret;

public class lightweightdownloader implements Runnable {
    JFrame mainFrame ;
    JTextArea textArea ;
    JTextField TFieldFileName, outTField, urlTField ;
    JButton button ;
    JProgressBar progressBar ;
    ArrayList range;
    long filesize, progress=0 ;
    final int ThreadNo = 10 ;
    String id = "" ;
    boolean done = false ;
    boolean running = false ;
    boolean resume = false ;
    boolean userResume = false ;
    String dirTemp = System.getProperty("java.io.tmpdir") + File.separator ;
    File dirHome = new File(System.getProperty("user.home") + File.separator + "Downloads" + File.separator );
    
    
    public static void main (String[] args) throws Throwable {
        lightweightdownloader application = new lightweightdownloader ();
        application.init();
    }
    
    public void finalize() { System.gc(); }
    
    void init () {
        mainFrame = new JFrame("Downloader");
        mainFrame.setLayout(new BoxLayout(mainFrame.getContentPane(),BoxLayout.PAGE_AXIS));
        
        JPanel panel1 = new JPanel(new FlowLayout());
        JPanel panel2 = new JPanel(new FlowLayout());
        JPanel panel3 = new JPanel(new FlowLayout());
        JPanel panel4 = new JPanel(new FlowLayout());
        JPanel panel5 = new JPanel(new FlowLayout());
        JPanel panel6 = new JPanel(new FlowLayout());
        
        JLabel urlLabel = new JLabel("URL ");
        panel1.add(urlLabel);
        
        urlTField = new JTextField(30);
        panel1.add(urlTField);
        
        button = new JButton();
        setButton(1);
        button.addActionListener(act);
        panel2.add(button);
        
        progressBar = new JProgressBar();
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        panel2.add(progressBar);
        
        textArea = new JTextArea ("Waiting for link ... ",5,30);
        textArea.setEditable(false);
        textArea.setBackground(null);
        JScrollPane scrollBar = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        textArea.setLineWrap(true);
        DefaultCaret ct = (DefaultCaret) textArea.getCaret();
        ct.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        textArea.setCaretPosition(textArea.getDocument().getLength());
        panel3.add(scrollBar);
        
        JLabel outLabel = new JLabel("Output Folder: ");
        panel4.add(outLabel) ;
        
        outTField = new JTextField(dirHome.getAbsolutePath(),20);
        panel4.add(outTField);
        
        JButton buttontBrowseFiles = new JButton("...");
        buttontBrowseFiles.addActionListener(act);
        buttontBrowseFiles.setPreferredSize(new Dimension(20,15));
        buttontBrowseFiles.setActionCommand("browse_files");
        panel4.add(buttontBrowseFiles);
        
        JLabel labelFileName = new JLabel("File Name: ");
        TFieldFileName = new JTextField(20);
        panel5.add(labelFileName);
        panel5.add(TFieldFileName);
        
        mainFrame.add(panel1);
        mainFrame.add(panel2);
        mainFrame.add(panel3);
        mainFrame.add(panel4);
        mainFrame.add(panel5);
        mainFrame.add(panel6);

        mainFrame.pack();
        mainFrame.setResizable(true);
        mainFrame.setLocation(500, 150);
        mainFrame.setVisible(true);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    void buttonClicked() throws MalformedURLException, IOException {
        initValues();
        URL downloadlink = new URL (urlTField.getText());
        HttpURLConnection hrc = (HttpURLConnection) downloadlink.openConnection();
        filesize = hrc.getContentLength();
        String filetype = hrc.getContentType();
        int responseCode = hrc.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK & running ) {
            textArea.setText("URL Validation - OK ");
            setFileName(downloadlink, filetype);
            getid();
            makefragments();
            textArea.append("\nFile size: " + (float)(Math.round(filesize/(1024*1024))) + " MB\nDownloading in " + range.size() + " fragments.");
            Thread thread_main = new Thread() { 
            @Override
            public void run () {
                try {
                    if (running) { mainThread(); }
                    if (done == true) { textArea.append("\n- - - - - - - - - - DONE - - - - - - - - - -"); }
                    else { textArea.append("\n- - - - - - - - - - INTERRUPTED - - - - - - - - - -"); }
                    setButton(1);
                } catch (IOException ex) { Logger.getLogger(lightweightdownloader.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Throwable ex) { Logger.getLogger(lightweightdownloader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            };
            int inp = JOptionPane.showConfirmDialog(null, textArea.getText() + "\n\nProceed to Download ?" ,"Confirm", JOptionPane.OK_CANCEL_OPTION);
            if (inp == 0) { thread_main.start(); }
        }
    }
    
    void initValues() {
        progress=0 ;
        String id = "" ;
        done = false ;
        resume = false ;
        userResume = false ;
        running = true ;
        TFieldFileName.setText(null);
    }
    
    void mainThread () throws IOException {
        setButton(0);
        if ((!checkFileAlreadyPresent()) && running ) {
            for (int a=0; a < ((range.size()<10?range.size():10)) ; a++) {
                if ( checkFragmentAlreadyPresent(a) & (!userResume) & running ) {
                    int in = JOptionPane.showConfirmDialog(null, "Previously Downloaded Fragments present ... \nResume that ?" ,"Confirm", JOptionPane.YES_NO_OPTION);
                    if (in == 0) { resume = true ; userResume = true ;}
                    if (in != 0) { resume = false ; userResume = true ;}
                    }
                }
            ExecutorService ex = Executors.newFixedThreadPool(ThreadNo);
            textArea.append("\nDownloading fragments (Total-" + range.size() +"): ");
            for (int i=0; i<range.size(); i++) {
                int  b = i;
                Runnable worker = new Thread( () -> {
                    try {
                        if (running) { actualDownload(b); }
                    } catch (IOException ex1) {
                        Logger.getLogger(lightweightdownloader.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                });
                if (running) { ex.execute(worker); }
            }
            ex.shutdown();
            while (!ex.isTerminated()) {}
            if (running) { merge(); }
        }
    }
    
    void getid() {
        String urlText = urlTField.getText() ;
        for (int a=0; id.length()<21 ; a+=97) {
            while (a >= urlText.length()) { a=a-urlText.length(); }
            if (!((urlText.charAt(a) > 47 & urlText.charAt(a) < 58) | (urlText.charAt(a) > 64 & urlText.charAt(a) < 91) | (urlText.charAt(a) > 96 & urlText.charAt(a) < 123))) {
                a+=2;
                continue;
            }
            id+=(urlText.charAt(a));
        }
    }
    
    void setButton(int a) {
        String buttonValue[] = new String[] {"PAUSE", "DOWNLOAD" } ;
        button.setText(buttonValue[a]);
        button.setActionCommand(buttonValue[a]);        
    }
    
    void setFileName(URL downloadlink, String filetype) { 
        String fileName = TFieldFileName.getText();
        String Textlink = downloadlink.toString().replaceAll("%\\S\\S", "_");
        String fx = filetype.replaceAll("/", ".");
        if ( fileName.isBlank() ) {
            fileName = Textlink.substring(Textlink.lastIndexOf("/") + 1, Textlink.length());
            if (fileName.length()>40) {fileName = fileName.substring(0, 40); }
            TFieldFileName.setText(fileName);
        }
        String tempn = TFieldFileName.getText();
        tempn = tempn.substring(tempn.lastIndexOf(".") + 1, tempn.length());
        if (tempn.length() > 10 | tempn.length() < 1) {
            TFieldFileName.setText(fileName + "." + fx);
        }
        textArea.append("\nFileName: " + TFieldFileName.getText());
    }
    
    boolean checkFileAlreadyPresent() {
        File outputpath = new File(outTField.getText());
        File fileName = new File(outputpath + File.separator + TFieldFileName.getText());
        while (fileName.exists() && (fileName.length()==filesize)) {
            int inx = JOptionPane.showConfirmDialog(null, "File Already present \nDownload another copy ?" ,"Confirm", JOptionPane.YES_NO_OPTION);
            if (inx == 0) {
                TFieldFileName.setText("(copy) " + TFieldFileName.getText());
                fileName = new File(outputpath + File.separator + TFieldFileName.getText());
            }
            if (inx != 0) {
                textArea.append("\nFile Already downloaded ...\nUser denided to Redownload");
                return true ;
            }
        }
        return false ;
    }
    
    void makefragments(){
        ArrayList <Integer> list = new ArrayList <> () ;
        for (int i=0; i<filesize; i+=1048576) { list.add(i); }
        list.add((int)filesize+1);

        range = new ArrayList <> ();
        for (int a=0; a<list.size()-1; a++) { range.add(list.get(a) +"-"+ (list.get(a+1)-1)); }
    }
    
    boolean checkFragmentAlreadyPresent (int a) {
        File filename = new File(dirTemp+id+a);
        if (filename.length() == 1048576 ) { return true ; }
        else { return false ; }
    }
    
    void actualDownload (int a) throws IOException {
        if (!checkFragmentAlreadyPresent(a) && running) {
            URL downloadLink = new URL(urlTField.getText());
            HttpURLConnection hrc = (HttpURLConnection) downloadLink.openConnection();
            hrc.setRequestProperty("Range", "bytes="+range.get(a));
            InputStream inputStream = hrc.getInputStream(); 
            File tempOutputFile = new File(dirTemp+id+a);
            FileOutputStream outputStream = new FileOutputStream(tempOutputFile);
            byte[] buffer = new byte[4096];
            int i = -1 ;
            while (((i = inputStream.read(buffer)) != -1) & running ) {
                outputStream.write(buffer,0,i);
                progress+=4096;
                progressBar.setValue((int)(progress*100/filesize));
            }
            outputStream.close();
            inputStream.close();
            outputStream = null ;
            inputStream = null ;
            buffer = null ;
            textArea.append("   " + (a+1));
        } else if (running & resume ) {
            progress+=(1048576);
            textArea.append("   (" + (a+1) +")");
        }
    }
    
    void merge() throws FileNotFoundException, IOException {
        textArea.append("\nAll Fragments downloaded ... \nMerging fragments in single file ...");
        File outputpath = new File(outTField.getText());
        if (!outputpath.exists()) { outputpath.mkdir(); }
        File filepathOutput = new File(outputpath + File.separator + TFieldFileName.getText());
        if (filepathOutput.exists()) { filepathOutput.delete(); }
        int b=0;
        byte[] fileBytes = null ; ;
        FileOutputStream fileOutputStream = new FileOutputStream(filepathOutput,true);
        for(int i=0; i<range.size(); i++) {
            File downloadedTempFile = new File(dirTemp + File.separator + id + i) ;
            InputStream inputStreamMerge = new FileInputStream(downloadedTempFile);
            fileBytes = new byte[(int) downloadedTempFile.length()];
            b = inputStreamMerge.read(fileBytes, 0, (int) downloadedTempFile.length());
            assert(b == fileBytes.length);
            assert(b == (int) downloadedTempFile.length());
            fileOutputStream.write(fileBytes);
            fileOutputStream.flush();
            fileBytes = null ;
            inputStreamMerge.close();
            inputStreamMerge = null;
            downloadedTempFile.delete();
        }
        fileOutputStream.close();
        fileOutputStream = null ;
        if ( filepathOutput.length() == filesize ) {
            textArea.append("\nFile size matched ... \n" + TFieldFileName.getText() + " Download Completed.");
            done = true ;
        }
        else { textArea.append("Error : File size doesn't match !!!"); }
        this.finalize();
    }
    
    void browseFile() {
        JFileChooser browseFileWindow = new JFileChooser(dirHome);
        browseFileWindow.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int i = browseFileWindow.showDialog(null, "Download here");
        if (i == 0) { outTField.setText(browseFileWindow.getSelectedFile().getPath()) ; }
    }
    
    ActionListener act = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("DOWNLOAD")) {
                try {
                    buttonClicked ();
                } catch (IOException ex) {
                    Logger.getLogger(lightweightdownloader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (e.getActionCommand().equals("PAUSE")) { running = false ; }
            
            if (e.getActionCommand().equals("browse_files")) { browseFile(); }
        }
    };

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    }