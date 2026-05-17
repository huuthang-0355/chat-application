package server.service;

import java.util.List;

import server.db.GroupDAO;

public class GroupService {
    private GroupDAO groupDAO = new GroupDAO();

    public String createGroup(String groupName, int creatorId) {
        if (groupName == null || groupName.isEmpty())
            return "Group name cannot be empty!";

        int existingId = groupDAO.getGroupIdByName(groupName);

        if (existingId != -1)
            return "Group name already taken";

        int newId = groupDAO.createGroup(groupName, creatorId);
        if (newId == -1)
            return "DB error creating group";

        // add creator into group as a member
        groupDAO.addMember(newId, creatorId);

        return "OK:" + String.valueOf(newId);
    }

    public String joinGroup(int groupId, int userId) {
        // check whether user in group
        if (groupDAO.isMember(groupId, userId))
            return "Already a member";

        boolean success = groupDAO.addMember(groupId, userId);

        if (success)
            return "OK";

        return "DB error joining group";
    }

    public String leaveGroup(int groupId, int userId) {
        // check whether user is in group
        if (!groupDAO.isMember(groupId, userId))
            return "User is not a member";

        boolean success = groupDAO.removeMember(groupId, userId);

        if (success)
            return "OK";

        return "DB error leaving group";
    }

    public String getUserGroupList(int userId) {

        List<String> groups = groupDAO.getUserGroups(userId);

        if (groups.size() == 0)
            return null;

        return String.join(",", groups); // "study:7,volleybal:7"
    }

    public boolean isMember(int groupId, int userId) {
        return groupDAO.isMember(groupId, userId);
    }
}
