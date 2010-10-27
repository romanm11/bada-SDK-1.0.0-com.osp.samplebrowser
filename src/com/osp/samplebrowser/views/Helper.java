package com.osp.samplebrowser.views;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Collections;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import com.osp.ide.IConstants;
import com.osp.ide.core.OspPropertyStore;
import com.osp.ide.utils.WorkspaceUtils;

public class Helper {
	private final static boolean BEFORE_ALPHA1 = false;
	private final static boolean IS_SDK = true;
	private static final boolean DELETE_TEMPORARY = true;
	
	private static final String BADA_DEV_PATH_VAR = "P4ROOT";
	//private static final String BADA_SDK_PATH_VAR = "BADA_SDK_HOME";
	
	private final static String OSP_CONF_FILE = ".badaprj";
	private final static String OSP_CONF = java.io.File.separatorChar + OSP_CONF_FILE;
	private final static String BADA_MODEL_FOLDER_WAVE = "Wave";
	private static final String BADA_SDK_MODEL_PATH = java.io.File.separatorChar + "Model";
	
	private final static int MAX_WAIT_TIME = 100000000;

	public static int depth;
	
	public static boolean doesDelete() {
		return DELETE_TEMPORARY;
	}
	
	public static boolean isSDK() {
		return IS_SDK;
	}
	
	public static String sdkRoot() {
		String sdkRoot;
		if (IS_SDK) 
			//sdkRoot = System.getenv(BADA_SDK_PATH_VAR);
			sdkRoot = WorkspaceUtils.getDefaultBadaSdkRoot();
		else
			sdkRoot = WorkspaceUtils.getDefaultBadaSdkRoot();
		return sdkRoot;
	}
	
	// not found -> return -1
	public static int findCProjectWithDepth(File src) {
		depth = -1;
		if (findCProject(src) == false) return -1;
		return depth;
	}
		
	public static boolean findCProject(File src) {
		// find ".cproject" to check ba:da project
		if (src.isDirectory()) {
			File[] childList = src.listFiles();
			
			for (int i = 0; i < childList.length; i++) {
				depth++;

				if (findCProject(childList[i]) == true) { 
					return true;
				} else {
					
				}
			}			
			
		} else {
			if (src.toString().endsWith(".cproject")) return true;
			else {
				depth--;
				return false;
			}
		}
		return false;
	}
	
	// before this method is called, 
	// you MUST call findCProject or findCProjectWithDepth 
	// to check if really .cproject exists
	public static File pathWithCProject(File src) {
		if (src.isDirectory()) {
			
			// if child is .cproject return current path
			File[] childList = src.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".cproject");
				}
			});
			
			// found 
			if (childList.length > 0) return src;
			
			// search deeper
			File returnPath;
			childList = src.listFiles();
			for (int i = 0; i < childList.length; i++) {
				returnPath = pathWithCProject(childList[i]);
				if (!returnPath.toString().equals("null"))
					return returnPath;				
			}
			
			return new File("null");
			
		} else {
			if (src.toString().endsWith(".cproject")) return src.getParentFile();
			else return new File("null");
		}
		
	}
	
	public static void dumplicateProject(File sourceDir, File targetDir) {
		System.out.println("- dumplicating project");
		System.out.println("-- " + sourceDir + " to " + targetDir.toString());
		
		copy(sourceDir, targetDir);
	}
	
	public static void copy(File source, File target) {
		if (source.isDirectory()) {
			if (!target.isDirectory()) {
				target.mkdir();
			}
			
			String[] filelist = source.list();
			for (int i = 0; i < filelist.length; i++) {
				copy(new File(source, filelist[i]), new File(target, filelist[i]));
			}
		} else {
			fileCopy(source, target);
		}		
	}
	
	public static void copyCDTConfigurationFiles(File src, File target)	{
		System.out.println("-- copying CDT configs from " + src + " to " + target);
		File cprojectF = new File(src, ".cproject");
		File newCprojectF = new File(target, ".cproject");
		fileCopy(cprojectF, newCprojectF);
		File projectF = new File(src, ".project");
		File newProjectF = new File(target, ".project");
		fileCopy(projectF, newProjectF);		
	}
	public static String copyConfigurationFiles(File src, File target) {
		
		System.out.println("-- copying configs from " + src + " to " + target);
		
		File ospF = new File(src, OSP_CONF_FILE);
		File newOspF = new File(target, OSP_CONF_FILE);
		fileCopy(ospF, newOspF);
		return newOspF.toString();
	}
	
	public static void fileCopy(File source, File target) {
		
		System.out.println("--- copy " + source.toString() + " to " + target.toString());
		
		FileInputStream inputStream = null;
		FileOutputStream outputStream = null;
		
		FileChannel inputChannel = null;
		FileChannel outputChannel = null;
		
		FileLock lock = null;
		
		try {
			inputStream = new FileInputStream(source);
			outputStream = new FileOutputStream(target);
			
			inputChannel = inputStream.getChannel();
			outputChannel = outputStream.getChannel();
			
			//lock = outputChannel.lock();
			
			long size = inputChannel.size();
			inputChannel.transferTo(0, size, outputChannel);
		} catch (Exception e){
			e.printStackTrace();
		} finally {
			try {
				inputChannel.close();
				outputChannel.close();
				inputStream.close();
				outputStream.close();
			} catch (IOException ioe) {}
			
			/*
			try {
				lock.release();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
		}
	}
	
	public static boolean rmDir(String dirName) {
		File tmpLocRoot = new File(dirName.toString());
		if (tmpLocRoot.exists()) {
			System.out.println("--- Deleting loc : " + tmpLocRoot);
			if (!tmpLocRoot.delete()) {
				// not empty > false
				if (!Helper.rmDir(tmpLocRoot)) {
					System.out.println("---- failed to delete!!!");
					return false;
				}
			}
		} 
		return true;
	}
	public static boolean rmDir(File deletedDir) {
		if (deletedDir.isDirectory()) {
			
			// delete children first
			boolean result = true;
			File[] childList = deletedDir.listFiles();
			for (int i = 0; i < childList.length; i++) {
				if (childList[i].isDirectory()) {
					result = rmDir(childList[i]);
				} else {
					result = delFile(childList[i]);
				}
				if (result == false) return false;
			}			
			
			// delete
			System.out.println("----- delete : " + deletedDir);
			result = deletedDir.delete();
			return result;			
		} else {
			return false;
		}
	}
	
	public static boolean delFile(File deletedFile)	{
		if (deletedFile.isDirectory()) {
			return false;			
		} else {
			System.out.println("----- delete : " + deletedFile);
			boolean result = deletedFile.delete();
			return result;			
		}
	}
	
	public static boolean modifyConfigurations(IProject proj) {
		boolean update = false;
		
		System.out.println("+ Modifying ");
		
		do {
			modifybadaConfigurations(proj);
			
			// check
			OspPropertyStore ospPropStore = new OspPropertyStore(proj);
			if (ospPropStore.getSdkPath().equals(sdkRoot())) {
				update = true;
				
				System.out.println("---- modifying : completed");				
			} else {
				ospPropStore.setSdkPath(sdkRoot());
				ospPropStore.store();
			}
		} while (!update);
		
		return true;		
	}
	public static boolean modifybadaConfigurations(IProject proj) {
		
		System.out.println("+ Modifying " + OSP_CONF_FILE);
		
		// To use bada IDE Class, OspPropertyStore
		
		IFile badaFile = proj.getFile(java.io.File.separatorChar + OSP_CONF_FILE);		
		System.out.println("+ " + badaFile.getFullPath());

		// SDK Root
		String sdkRoot = sdkRoot();		

		OspPropertyStore ospPropStore = new OspPropertyStore(proj);
		// call ospPropStore.load(proj) in OspPropertyStore constructor
		//ospPropStore.load(proj);
		
		/*
		int tmpCnt = 0;
		while (ospPropStore.getAppName().equals("") && tmpCnt < MAX_WAIT_TIME) {
			tmpCnt++;
			ospPropStore.load(proj);
		}		
		System.out.println("-- wait count : " + tmpCnt);	
		
		if (ospPropStore.getSdkPath().equals(sdkRoot))
			System.out.println("--- SDK root : same " + sdkRoot);
		else
			System.out.println("--- SDK root : different " + sdkRoot + ", " + ospPropStore.getSdkPath());		
		
		if (tmpCnt >= MAX_WAIT_TIME) {
			return false;		
		}
		*/		
		
		/*
		ospPropStore.setProjectType(ospPropStore.getProjectType());		
		
		String entry = ospPropStore.getEntry();
		String name = ospPropStore.getAppName();
		String version = ospPropStore.getVersion();
		
		System.out.println("-- name : " + name);		
		
		ospPropStore.setEntry(entry);
		ospPropStore.setAppName(name);
		ospPropStore.setVersion(version);
		
		String icon = ospPropStore.getIconMainMenu();		
		ospPropStore.setIconMainMenu(icon);
		
		if (!BEFORE_ALPHA1) {
			String iconSetting = ospPropStore.getIconSetting();
			String iconTicker = ospPropStore.getIconTicker();
			String iconQuickPanel = ospPropStore.getIconQuickPanel();
			String iconLoading = ospPropStore.getIconLoading();
			
			ospPropStore.setIconSetting(iconSetting);
			ospPropStore.setIconTicker(iconTicker);
			ospPropStore.setIconQuickPanel(iconQuickPanel);
			ospPropStore.setIconLoading(iconLoading);			
		}
		
		String vendor = ospPropStore.getVendor();
		String description = ospPropStore.getDescription();		
		
		ospPropStore.setVendor(vendor);
		ospPropStore.setDescription(description);
		*/
		
		// BEFORE 1.0.0a2 ( ~ 1.0.0a1)
		/* 
		if (entry.equals(""))
			ospPropStore.setEntry(appXmlStore.getEntry());			
		else
			ospPropStore.setEntry(entry);
		if (name.equals(""))
			ospPropStore.setAppName(appXmlStore.getName());
		else
			ospPropStore.setAppName(name);
		if (version.equals(""))
			ospPropStore.setVersion(appXmlStore.getVersion());
		else 
			ospPropStore.setVersion(version);
		if (icon.equals(""))
			ospPropStore.setIconMainMenu(appXmlStore.getIconMainMenu());
		else
			ospPropStore.setIconMainMenu(icon);
		if (vendor.equals(""))
			ospPropStore.setVendor(appXmlStore.getVendor());
		else 
			ospPropStore.setVendor(vendor);
		if (description.equals(""))
			ospPropStore.setDescription(appXmlStore.getDescription());
		else
			ospPropStore.setDescription(description);
			*/
		
		ospPropStore.setSdkPath(sdkRoot);
		
		//if (ospPropStore.getModel().equals(BADA_MODEL_FOLDER_WAVE)) {

			File root = new File(sdkRoot + BADA_SDK_MODEL_PATH);
			String BadaModel = ospPropStore.getModel();
			
			if (root != null && root.exists()) {
				File[] files = root.listFiles();

				Vector<String> models = new Vector<String>();
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						String name = files[i].getName();

						if(files[i].getName().toUpperCase().equals(BadaModel.toUpperCase())){
						//if(files[i].getName().equals(BadaModel)){
							models.clear();
							break;
						}
						models.add(files[i].getName());
					}
				}
				Collections.sort(models);

				// when not found, modify to the model as default	
				if (models.size() > 0)					
				{
					//ospPropStore.setModel(models.get(0));
					String strModel;
					boolean bFound = false;
					
					// 1st priority : Wave
					strModel = "Wave";
					for (int i = 0; i < models.size(); i++)
					{
						if (strModel.toUpperCase().equals(models.get(i).toUpperCase())) 
						{
							bFound = true;
							break;
						}						
					}
					
					// 2nd priority : Wave_*
					if (!bFound) 
					{
						strModel = "Wave_";
						for (int i = 0; i < models.size(); i++)
						{
							if (models.get(i).toUpperCase().startsWith(strModel.toUpperCase()))
							{
								strModel = models.get(i);
								bFound = true;
								break;
							}
						}
						
					}
					
					// 3rd priority : Scotia
					if (!bFound) 
					{
						strModel = "Scotia";
						for (int i = 0; i < models.size(); i++)
						{
							if (strModel.toUpperCase().equals(models.get(i).toUpperCase())) 
							{
								bFound = true;
								break;
							}						
						}						
					}
					
					// 4th priority : Scotia_*
					if (!bFound) 
					{
						strModel = "Scotia_";
						for (int i = 0; i < models.size(); i++)
						{
							if (models.get(i).toUpperCase().startsWith(strModel.toUpperCase()))
							{
								strModel = models.get(i);
								bFound = true;
								break;
							}
						}
						
					}
					
					// if not found
					if (!bFound)
						strModel = models.get(0);
					
					ospPropStore.setModel(strModel);
				}
			}
		//}
		ospPropStore.store();

		System.out.println("-- SDK Path : " + ospPropStore.getSdkPath());

		System.out.println("-- modify : success");
		return true;
	}	
}