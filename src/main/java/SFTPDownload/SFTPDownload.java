package SFTPDownload;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Vector;

public class SFTPDownload extends JFrame {
    private JButton downloadButton;

    public SFTPDownload() {
        setTitle("SFTP Folder Downloader");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        downloadButton = new JButton("Download Folder");
        downloadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onDownloadClick();
            }
        });

        add(downloadButton);
        setLocationRelativeTo(null);
    }

    private void onDownloadClick() {
        String host = "admission.dei.ac.in";
        int port = 22;
        String username = "examadmin";
        String password = "G00disLife@2024";
        String remoteFolder = "/home/examadmin/dglocker/";

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = chooser.showSaveDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File localFolder = chooser.getSelectedFile();
            try {
                JSch jsch = new JSch();
                Session session = jsch.getSession(username, host, port);
                session.setPassword(password);
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();

                ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
                sftpChannel.connect();

                // Step 1: List available sessions (subdirectories) and select one
                String selectedSession = selectSession(sftpChannel, remoteFolder);
                if (selectedSession != null) {
                    // Step 2: Download the selected session folder
                    downloadFolder(sftpChannel, selectedSession, localFolder.getAbsolutePath());
                    JOptionPane.showMessageDialog(this, "Folder downloaded successfully!");
                } else {
                    JOptionPane.showMessageDialog(this, "No session selected.", "Warning", JOptionPane.WARNING_MESSAGE);
                }

                sftpChannel.disconnect();
                session.disconnect();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private String selectSession(ChannelSftp sftpChannel, String remoteFolder) throws SftpException {
        Vector<ChannelSftp.LsEntry> list = sftpChannel.ls(remoteFolder);
        Vector<String> sessionFolders = new Vector<>();

        System.out.println("Listing directories in: " + remoteFolder);

        for (ChannelSftp.LsEntry entry : list) {
            if (entry.getAttrs().isDir() && !entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
                System.out.println("Found directory: " + entry.getFilename());
                sessionFolders.add(remoteFolder + "/" + entry.getFilename());
            }
        }

        if (sessionFolders.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No sessions found in the remote folder.", "Warning", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        System.out.println("Available sessions: " + sessionFolders);

        String selectedSession = (String) JOptionPane.showInputDialog(
                this,
                "Select Session:",
                "Session Selection",
                JOptionPane.QUESTION_MESSAGE,
                null,
                sessionFolders.toArray(),
                sessionFolders.get(0)
        );

        System.out.println("Selected session: " + selectedSession);

        return selectedSession;
    }

    private void downloadFolder(ChannelSftp sftpChannel, String remoteFolder, String localFolder) throws SftpException {
        Vector<ChannelSftp.LsEntry> list = sftpChannel.ls(remoteFolder);
        new File(localFolder).mkdirs(); // Create local folder if it doesn't exist

        for (ChannelSftp.LsEntry entry : list) {
            if (entry.getAttrs().isDir()) {
                if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
                    downloadFolder(sftpChannel, remoteFolder + "/" + entry.getFilename(), localFolder + "/" + entry.getFilename());
                }
            } else {
                downloadFile(sftpChannel, remoteFolder + "/" + entry.getFilename(), localFolder + "/" + entry.getFilename());
            }
        }
    }

    private void downloadFile(ChannelSftp sftpChannel, String remoteFile, String localFile) throws SftpException {
        try (OutputStream os = new FileOutputStream(localFile)) {
            sftpChannel.get(remoteFile, os);
        } catch (Exception e) {
            throw new SftpException(ChannelSftp.SSH_FX_FAILURE, "Failed to download file: " + remoteFile, e);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SFTPDownload().setVisible(true);
            }
        });
    }
}
