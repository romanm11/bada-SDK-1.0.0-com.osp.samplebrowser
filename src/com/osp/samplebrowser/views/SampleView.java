package com.osp.samplebrowser.views;

import org.eclipse.cdt.core.CCorePlugin;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;

import com.osp.ide.core.OspPropertyStore;
import com.osp.ide.utils.WorkspaceUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

import javax.swing.filechooser.FileFilter;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class SampleView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.osp.samplebrowser.views.SampleView";	

	private static String gdbinitPath = null;
	private static final String gdbinitPath2 = "\\OspdSdk\\BadaDev\\";
	private static final String gdbinitFilename = ".gdbinit";
	private static String selected = null;
	
	private static final String BADA_DEV_PATH_VAR = "P4ROOT";
	private static final String BADA_DEV_SAMPLE_PATH = "\\OspdApp\\Samples";
	//private static final String BADA_SDK_PATH_VAR = "BADA_SDK_HOME";
	private static final String BADA_SDK_SAMPLE_PATH = "\\Samples";
	private final static String OSP_CONF_FILE = ".badaprj";
	
	private static final String PREFIX_TEMP = "_tmp_";
	
	final int MAX_WAIT_TIME = 10000;

	private TableViewer viewer;
	
	private Action copyProjectAction;
	
	private Action emptyAction;
	private Action testAction;
	//private Action action2;
	//private Action doubleClickAction;

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	 
	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {

			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceRoot root = workspace.getRoot();
			
			// find "_tmp_" to delete in My workspace
			// project stored in reverse order
			/*
			IProject[] projectList = root.getProjects();
			if (projectList[projectList.length-1].getName().startsWith("_")) {
				for (int cntProjectList = projectList.length-1; cntProjectList >= 0; cntProjectList--) {
					String name = projectList[cntProjectList].getName();
					System.out.println("Project : " + name);
					
					if (!name.startsWith("_")) break;
					
					if (name.startsWith("_tmp_")) {
						// delete temporary
						final IProject tmpProject = root.getProject(name);
						try {
							tmpProject.delete(true, new NullProgressMonitor());
						} catch (CoreException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}							
					}
				}
			}
			*/

			// SDK root path 
			String sdkPath = null;			
			File samplePath = null;
			if (Helper.isSDK()) {
				//sdkPath = System.getenv(BADA_SDK_PATH_VAR);
				//samplePath = new File(sdkPath + BADA_SDK_SAMPLE_PATH);
				sdkPath = WorkspaceUtils.getDefaultBadaSdkRoot();
				samplePath = new File(sdkPath + BADA_SDK_SAMPLE_PATH);
			} else {
				//sdkPath = System.getenv(BADA_DEV_PATH_VAR);
				//samplePath = new File(sdkPath + BADA_DEV_SAMPLE_PATH);
				sdkPath = WorkspaceUtils.getDefaultBadaSdkRoot();
				samplePath = new File(sdkPath + BADA_SDK_SAMPLE_PATH);
			}
			System.out.println("bada SDK root : " + sdkPath);			
			
			if (!Helper.isSDK() && false) {
				gdbinitPath = sdkPath + gdbinitPath2 + gdbinitFilename;
			}
			
			//System.out.println("Current path : " + samplePath.getPath());
			//System.out.println("Current Absolute Path : " + samplePath.getAbsolutePath());
			
			File[] sampleList = samplePath.listFiles();
			ArrayList<String> sampleNameList = new ArrayList<String>(); 
			
			if (sampleList != null) {
				int j = 1;
				for (int i = 0; i < sampleList.length; i++) {
					if (sampleList[i].isDirectory()) {
						
						//System.out.print("+ " + sampleList[i].toString());						
						
						// find ".cproject" to check ba:da project
						/* Just consider 1 level depth
						File[] childList = sampleList[i].listFiles(new FilenameFilter() {
							public boolean accept(File dir, String name) {
								return name.endsWith(".cproject");
							}
						});						
						
						if (childList.length > 0) {
							// ba:da project
							System.out.println(" [ba:da]");
							
							sampleNameList.add(sampleList[i].getName());
							j++;						
						} else {
							System.out.println(" [X]");
						}
						*/
						
						int depth = Helper.findCProjectWithDepth(sampleList[i]);
						if ( depth >= 0) {
							File projectRoot = Helper.pathWithCProject(sampleList[i]);
							boolean existProject = false;
							boolean existManifest = false;
							boolean existApplicationXML = false;
							
							File[] childProject = projectRoot.listFiles();
							for (int k = 0; k < childProject.length; k++) {
								String childName = childProject[k].getName();
								if (childName.equals(".project"))
									existProject = true;
								if (childName.equalsIgnoreCase("manifest.xml"))
									existManifest = true;
								if (childName.equalsIgnoreCase("application.xml"))
									existApplicationXML = true;
							}							
							
							if (existProject && existManifest && existApplicationXML) {
								// bada project
								//System.out.println(" [bada] " + depth);
								
								sampleNameList.add(sampleList[i].getName());
								j++;
							}
						} else {
							//System.out.println(" [X] " + depth);
						}						
					}					
				}				
			} else {
				//System.out.println("No childrens");
			}
			
			String[] sampleNames = new String[sampleNameList.size()];
			sampleNameList.toArray(sampleNames);
			
			for (int i = 0; i < sampleNames.length; i++) {
				//System.out.println(sampleNames[i]);
			}
			
			return sampleNames;
		}
	}
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}
		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}
		public Image getImage(Object obj) {
			// for entry's icon
			return PlatformUI.getWorkbench().
					getSharedImages().getImage(ISharedImages.IMG_OBJ_PROJECT_CLOSED);
		}
	}
	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */
	public SampleView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "com.osp.modifysamples.viewer");
		makeActions();
		hookContextMenu();
		//hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				SampleView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		//manager.add(copyProjectAction);
		//manager.add(testAction);
		/*
		manager.add(new Separator());
		manager.add(action2);
		*/
	}

	private void fillContextMenu(IMenuManager manager) {		
		manager.add(copyProjectAction);
		//manager.add(testAction);
		/*
		manager.add(action2);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		*/
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		//manager.add(emptyAction);
		//manager.add(copyProjectAction);
		//manager.add(testAction);
		/*
		manager.add(action2);
		*/
	}

	private void makeActions() {
		/*
		emptyAction = new Action() {
			
		};
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		String testString = root.getLocation().toString();		
		emptyAction.setText(testString);
		*/
		
		copyProjectAction = new Action() {
			public void run() {
				
				// Selected App
				ISelection selection = viewer.getSelection();
				
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				selected = obj.toString();
				
				System.out.println("====== " + selected + " =====");
				
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IWorkspaceRoot root = workspace.getRoot();
				IPath workLoc = root.getLocation();
						
				// check if existing temporaries
				IProject[] projectList = root.getProjects();
				if (projectList.length > 0) {
					System.out.println("--- " + projectList[projectList.length - 1].getName().toString());
					/*
					// partial search
					if (projectList[projectList.length - 1].getName().toString().startsWith("_")) {
						for (int i = projectList.length - 1; i >= 0; i--) {						
							if (projectList[i].getName().toString().startsWith(PREFIX_TEMP)) {
								if (projectList[i].exists())
									try {
										projectList[i].delete(true, new NullProgressMonitor());
									} catch (CoreException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
							}
						}
					}
					*/
					for (int i = 0; i < projectList.length; i++) {
						if (projectList[i].getName().toString().startsWith(PREFIX_TEMP)) {
							if (projectList[i].exists() && Helper.doesDelete()) {
								try {
									projectList[i].delete(true, new NullProgressMonitor());
									IPath tmpPath = workLoc.append(projectList[i].getName());
									Helper.rmDir(tmpPath.toString());
								} catch (CoreException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}								
							}
						}
					}
				}
				
				// check if same project exists 
				IProject project4Check = root.getProject(selected);
				if (project4Check.exists()) {
					showMessage(selected + " already exists...");
					return;
				}
				
				IPath projLoc = workLoc.append(selected);
				
				String targetPath = projLoc.toString();
				File targetDir = new File(targetPath);				
				
				if (targetDir.exists()) {
					showMessage(targetPath + " already exists...");
				} else {
					
					String tmpProjectName = PREFIX_TEMP + selected;
				
					// find source path
					String sdkPath = null;
					String sourcePath = null;

					sdkPath = WorkspaceUtils.getDefaultBadaSdkRoot();

					if (Helper.isSDK())
						sourcePath = sdkPath + BADA_SDK_SAMPLE_PATH + "\\" + selected;
					else
						sourcePath = sdkPath + BADA_DEV_SAMPLE_PATH + "\\" + selected;
			
					File samplePath = new File(sourcePath);
					int depth = Helper.findCProjectWithDepth(samplePath);
					
					if (depth != 0) {
						samplePath = Helper.pathWithCProject(samplePath);
						sourcePath = samplePath.toString();
					}					
					
					// temporary path				
					IPath tmpLoc = workLoc.append(tmpProjectName);
					File tmpPath = new File(tmpLoc.toString());					
					
					System.out.println("= Source : " + sourcePath);
					System.out.println("= Temp   : " + tmpPath);
					System.out.println("= Target : " + targetDir);				
					
					try {
						if (tmpPath.exists()) { 
							System.out.println("- removing existed temporaries");
							// MSYS
							/*
							Process p = Runtime.getRuntime().exec("rm -rf " + tmpPath.toString());
							int result = p.waitFor();
							if (result == 0)
								System.out.println("-- successed : rm -rf " + tmpPath);
							else
								System.out.println("-- failed : rm -rf " + tmpPath);
							*/
							Process p = Runtime.getRuntime().exec("rm -rf " + tmpPath.toString() + "\\" + OSP_CONF_FILE);
							int result = p.waitFor();
							if (result == 0)
								System.out.println("-- successed : rm -rf " + tmpPath.toString() + "\\" + OSP_CONF_FILE);
							else
								System.out.println("-- failed : rm -rf " + tmpPath.toString() + "\\" + OSP_CONF_FILE);
							
							p = Runtime.getRuntime().exec("rm -rf " + tmpPath.toString() + "\\.cproject");
							result = p.waitFor();
							if (result == 0)
								System.out.println("-- successed : rm -rf " + tmpPath.toString() + "\\.cproject");
							else
								System.out.println("-- failed : rm -rf " + tmpPath.toString() + "\\.cproject");
							
							p = Runtime.getRuntime().exec("rm -rf " + tmpPath.toString() + "\\.project");
							result = p.waitFor();
							if (result == 0)
								System.out.println("-- successed : rm -rf " + tmpPath.toString() + "\\.project");
							else
								System.out.println("-- failed : rm -rf " + tmpPath.toString() + "\\.project");
						}
						/*
						tmpPath.mkdir();
						Runtime.getRuntime().exec("cp -f " + sourcePath + "\\.cproject " + tmpPath + "\\");
						Runtime.getRuntime().exec("cp -f " + sourcePath + "\\.project " + tmpPath + "\\");
						Runtime.getRuntime().exec("cp -f " + sourcePath + "\\.osp " + tmpPath + "\\");
						Runtime.getRuntime().exec("cp -f " + sourcePath + "\\application.xml " + tmpPath + "\\");
						*/
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					System.out.println("=== TEMPORARY ===");
					
					System.out.println("- copying configs from " + samplePath + " to " + tmpPath);
					
					tmpPath.mkdir();
					String resultFile = Helper.copyConfigurationFiles(samplePath, tmpPath);
					Helper.copyCDTConfigurationFiles(samplePath, tmpPath);
					
					System.out.println("-- Osp Conf File : " + resultFile);
					
					System.out.println("- modifying : " + tmpPath);										
					
					//
					transferToTemporary();
					

					System.out.println("=== DUPLICATED ===");					
					
					// copy whole files - w/o Shell
					System.out.println("- copying whole files " + sourcePath + " to " + targetDir);					
					// only for using Shell
					//targetDir.mkdir();
					Helper.dumplicateProject(samplePath, targetDir);
					
					// copy modified configs from temporary					
					System.out.println("- copying configs from " + tmpPath + " to " + targetDir);					
					Helper.copyConfigurationFiles(tmpPath, targetDir);					

					System.out.println("- opening new project");

					transfer();
					
					// delete temporary
					System.out.println("- deleting temporary");
				
					final IProject tmpProject = root.getProject(tmpProjectName);
					if (tmpProject.exists() && Helper.doesDelete()) {
						try {
							IPath tmpDirPath = workLoc.append(tmpProject.getName());
							tmpProject.open(new NullProgressMonitor());
							tmpProject.close(new NullProgressMonitor());
							tmpProject.delete(true, new NullProgressMonitor());
							Helper.rmDir(tmpDirPath.toString());
						} catch (CoreException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					
					if (!Helper.isSDK() && false) {
						// copy .gdbinit to project root
						File gdbinitFile = new File(gdbinitPath);				
						File newGdbinitFile = new File(targetDir.toString() + "\\" + gdbinitFilename);
						System.out.println("-- copy : " + gdbinitFile + " -> " + newGdbinitFile);
						if (gdbinitFile.exists() && !newGdbinitFile.exists()) {
							Helper.fileCopy(gdbinitFile, newGdbinitFile);
						}
					}
						
					System.out.println("= COMPLETED");					
				}
			}
		};
	
		copyProjectAction.setText("Copy into my workspace...");
		//copyProjectAction.setToolTipText("Copy into my workspace...");
		//importAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
//			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		
		/*
		testAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				
				//System.out.println(obj.toString());
				
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IWorkspaceRoot root = workspace.getRoot();
				IPath workLoc = root.getLocation();
				IPath projLoc = workLoc.append(obj.toString());
				
				//System.out.println("Currnet workspace : " + workLoc.toString());
				//System.out.println("Current project : " + projLoc.toString());
				
				String sourcePath = System.getenv(BADA_PATH_VAR) + SAMPLE_PATH + "\\" + obj.toString();
				String targetPath = projLoc.toString();
				File sourceDir = new File(sourcePath);
				File targetDir = new File(targetPath);
				IProject proj = root.getProject(obj.toString());
				try {
					//Helper.modifyConfigurations(proj, sourceDir, targetDir);
					Helper.modifyConfigurations(proj);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};		
		
		testAction.setText("Test...");
		*/
		
		/*		
		action2 = new Action() {
			public void run() {
				showMessage("Action 2 executed");
			}
		};
		action2.setText("Action 2");
		action2.setToolTipText("Action 2 tooltip");
		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		 
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				showMessage("Double-click detected on "+obj.toString());
			}
		};
		*/
	}

	/*
	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}
	*/
	private void showMessage(String message) {
		MessageDialog.openWarning(
		//MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Sample Browser",
			message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	private void transferToTemporary() {
		String targetName = PREFIX_TEMP + selected;
		
		//System.out.println(targetName);
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();		
				
		final IProject importedProject = root.getProject(targetName);
		IProject newProject = null;
		
		System.out.println("+ temporary project : " + importedProject.getName() + " (" + targetName + " )");
		
		if (importedProject.exists()) {
			System.out.println(importedProject.getName() + " is already imported...");
		} else {
			System.out.println("-- " + targetName + " : not exist");
			
			IProjectDescription description = workspace.newProjectDescription(importedProject.getName());						
			//description.setLocation(projLoc);

			System.out.println("-- description is created");
			
			try {
				newProject = CCorePlugin.getDefault().createCDTProject(description, importedProject, new NullProgressMonitor());
			} catch (OperationCanceledException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (CoreException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

			System.out.println("-- " + newProject.getName() + " : created");
			
			if (newProject != null) {
				if (!newProject.isOpen()) {
					try {
						newProject.open(new NullProgressMonitor());
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}							

			System.out.println("-- project : " + newProject.getName() + " : opened");

			try {
				importedProject.open(new NullProgressMonitor());
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println("-- project : " + importedProject.getName() + " : opened");			

			try {
				if (!Helper.modifyConfigurations(newProject)) {
					// clear temporary files
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// double check 
			OspPropertyStore tmpStore = new OspPropertyStore(newProject);
			System.out.println("++ modified SDK ROOT (" + newProject + ") : " + tmpStore.getSdkPath());
			
			String sdkRoot = Helper.sdkRoot();
			
			if (tmpStore.getSdkPath().equals(sdkRoot)) {
				System.out.println("-- modified : success");				
			} else {
				tmpStore.setSdkPath(sdkRoot);
				tmpStore.store();
			}
			
			System.out.println("-- " + newProject.getName() + " : modified");
		}		
	}
	
	private void transfer() {
		//showMessage("import...");
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		
		String sourceName = PREFIX_TEMP + selected;
		
		final IProject importedProject = root.getProject(selected);
		IProject newProject = null;
		
		System.out.println("+ new project : " + importedProject.getName());
		
		IProjectDescription description = workspace.newProjectDescription(importedProject.getName());						
		//description.setLocation(projLoc);
		
		System.out.println("-- description created");
		
		try {
			newProject = CCorePlugin.getDefault().createCDTProject(description, importedProject, new NullProgressMonitor());
		} catch (OperationCanceledException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (CoreException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		System.out.println("-- project : " + importedProject.getName() + " : created");
		
		if (newProject != null) {
			if (!newProject.isOpen()) {
				try {
					newProject.open(new NullProgressMonitor());
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}							

		System.out.println("-- project : " + newProject.getName() + " : opened");

		try {
			importedProject.open(new NullProgressMonitor());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("-- project : " + importedProject.getName() + " : opened");

		try {
			if (!Helper.modifyConfigurations(newProject)) {
				// clear temporary files
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("-- project : " + newProject.getName() + " : modified");

		// delete temporary project		
		final IProject tmpProject = root.getProject(sourceName);
		IPath tmpLoc = root.getLocation().append(tmpProject.getName());
		if (Helper.doesDelete()) {
			try {
				System.out.println("--- Deleting project : " + tmpProject);
				tmpProject.open(new NullProgressMonitor());
				//MODIFIED 2010.03.12 - Suwan Jeon, 닫힌 Resource를 삭제해서 예외가 발생한다. 삭제된 Resource는 닫을 필요 없는 것 같음.
				tmpProject.close(new NullProgressMonitor());
				
				// From this point, open files close automatically
				
				tmpProject.delete(true, new NullProgressMonitor());
			} catch (CoreException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			// Until this point, open files close automatically
			
			// delete temporary location 
			Helper.rmDir(tmpLoc.toString());
			
			System.out.println("-- temporary project : deleted");
		}
	}
}