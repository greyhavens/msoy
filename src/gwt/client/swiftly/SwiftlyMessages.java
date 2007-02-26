package client.swiftly;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	/export/msoy/src/gwt/client/swiftly/SwiftlyMessages.properties'.
 */
public interface SwiftlyMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "Log in to view your Swiftly projects.".
   * 
   * @return translated "Log in to view your Swiftly projects."
   * @gwt.key indexLogon
   */
  String indexLogon();

  /**
   * Translated "Not a valid projectId: {0}".
   * 
   * @return translated "Not a valid projectId: {0}."
   * @gwt.key invalidProjectId
   */
  String invalidProjectId(String arg0);

  /**
   * Translated "You have not created any projects.".
   * 
   * @return translated "You have not created any projects."
   * @gwt.key noMembersProjects
   */
  String noMembersProjects();

  /**
   * Translated "No remixable projects found.".
   * 
   * @return translated "No remixable projects found."
   * @gwt.key noRemixableProjects
   */
  String noRemixableProjects();

  /**
   * Translated "No project types found.".
   * 
   * @return translated "No project types found."
   * @gwt.key noTypes
   */
  String noTypes();

  /**
   * Translated "No collaborators found.".
   * 
   * @return translated "No collaborators found."
   * @gwt.key noCollaborators
   */
  String noCollaborators();

  /**
   * Translated "Collaborators:".
   * 
   * @return translated "Collaborators:"
   * @gwt.key collaborators
   */
  String collaborators();

  /**
   * Translated "Create Project.".
   * 
   * @return translated "Create Project."
   * @gwt.key createProject
   */
  String createProject();

  /**
   * Translated "Start a project!"
   * 
   * @return translated "Start a project!"
   * @gwt.key startProject
   */
  String startProject();

  /**
   * Translated "Project name"
   * 
   * @return translated "Project name"
   * @gwt.key projectName
   */
  String projectName();

  /**
   * Translated "Remixable?"
   * 
   * @return translated "Remixable?"
   * @gwt.key remixable
   */
  String remixable();

  /**
   * Translated "Your projects:".
   * 
   * @return translated "Your projects:"
   * @gwt.key membersProjects
   */
  String membersProjects();

  /**
   * Translated "Remixable projects:".
   * 
   * @return translated "Remixable projects:"
   * @gwt.key remixableProjects
   */
  String remixableProjects();

  /**
   * Translated "What type of project is this?".
   * 
   * @return translated "What type of project is this?"
   * @gwt.key selectType
   */
  String selectType();

  /**
   * Translated "You are Swiftly editing:".
   * 
   * @return translated "You are Swiftly editing:"
   * @gwt.key swiftlyEditing
   */
  String swiftlyEditing();

  /**
   * Translated "Edit Project".
   * 
   * @return translated "Edit Project"
   * @gwt.key editProject
   */
  String editProject();

  /**
   * Translated "Submit".
   * 
   * @return translated "Submit"
   * @gwt.key submit
   */
  String submit();

  /**
   * Translated "Cancel".
   * 
   * @return translated "Cancel"
   * @gwt.key cancel
   */
  String cancel();

  /**
   * Translated "Add Collaborators".
   * 
   * @return translated "Add Collaborators"
   * @gwt.key addCollaborators
   */
  String addCollaborators();

  /**
   * Translated "View profile".
   * 
   * @return translated "View profile"
   * @gwt.key viewProfile
   */
  String viewProfile();

  /**
   * Translated "Remove".
   * 
   * @return translated "Remove"
   * @gwt.key viewRemove
   */
  String viewRemove();

  /**
   * Translated "Are you sure you wish to remove {0} from {1}?".
   * 
   * @return translated "Are you sure you wish to remove {0} from {1}?"
   * @gwt.key viewRemovePrompt
   */
  String viewRemovePrompt(String arg0,  String arg1);
}
