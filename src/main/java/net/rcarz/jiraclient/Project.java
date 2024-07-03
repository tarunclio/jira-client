/**
 * jira-client - a simple JIRA REST client
 * Copyright (c) 2013 Bob Carroll (bob.carroll@alum.rit.edu)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.rcarz.jiraclient;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Represents a JIRA project.
 */
public class Project extends Resource {

    private Map<String, String> avatarUrls = null;
    private String key = null;
    private String name = null;
    private String description = null;
    private User lead = null;
    private String assigneeType = null;
    private List<Component> components = null;
    private List<IssueType> issueTypes = null;
    private List<Version> versions = null;
    private Map<String, String> roles = null;

    /**
     * Creates a project from a JSON payload.
     *
     * @param restclient REST client instance
     * @param json JSON payload
     */
    protected Project(RestClient restclient, JSONObject json) {
        super(restclient);

        if (json != null && !json.isEmpty())
            deserialise(json);
    }

    private void deserialise(JSONObject json) {

        self = json.optString("self");;
        id = json.optString("id");
        avatarUrls = Field.getMap(String.class, String.class, json.get("avatarUrls"));
        key = json.optString("key");
        name = json.optString("name");
        description = json.optString("description","");
        lead = Field.getResource(User.class,json.optJSONObject("lead",new JSONObject("{}")), restclient);
        assigneeType = json.optString("assigneeType","");
   
        components = Field.getResourceArray(Component.class, json.optJSONArray("components", new JSONArray()), restclient);
        issueTypes = Field.getResourceArray(
            IssueType.class,
            json.has("issueTypes") ? json.get("issueTypes") : json.optJSONArray("issuetypes"),
            restclient);
        versions = Field.getResourceArray(Version.class, json.optJSONArray("versions"), restclient);
        roles = Field.getMap(String.class, String.class, json.optJSONObject("roles"));
    }

    /**
     * Retrieves the given project record.
     *
     * @param restclient REST client instance
     * @param key Project key
     *
     * @return a project instance
     *
     * @throws JiraException when the retrieval fails
     */
    public static Project get(RestClient restclient, String key)
        throws JiraException {

        JSONObject result = null;

        try {
            result = restclient.get(getBaseUri() + "project/" + key);
        } catch (Exception ex) {
            throw new JiraException("Failed to retrieve project " + key, ex);
        }

        if (!(result instanceof JSONObject))
            throw new JiraException("JSON payload is malformed");

        return new Project(restclient, (JSONObject)result);
    }

    /**
     * Retrieves all project records visible to the session user.
     *
     * @param restclient REST client instance
     *
     * @return a list of projects
     *
     * @throws JiraException when the retrieval fails
     */
    public static List<Project> getAll(RestClient restclient) throws JiraException {
        Object result = null;

        try {
            result = restclient.get(getBaseUri() + "project");
        } catch (Exception ex) {
            throw new JiraException("Failed to retrieve projects", ex);
        }

        if (!(result instanceof JSONArray) || !(result instanceof JSONObject))
            throw new JiraException("JSON payload is malformed");

        return Field.getResourceArray(Project.class, result, restclient);
    }

    @Override
    public String toString() {
        return getName();
    }

    public Map<String, String> getAvatarUrls() {
        return avatarUrls;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public User getLead() {
        return lead;
    }

    public String getAssigneeType() {
        return assigneeType;
    }

    public List<Component> getComponents() {
        return components;
    }

    public List<IssueType> getIssueTypes() {
        return issueTypes;
    }

    public List<Version> getVersions() {
        return versions;
    }

    public Map<String, String> getRoles() {
        return roles;
    }
}

