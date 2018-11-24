import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.Spring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


import org.eclipse.egit.github.core.*;
import com.google.gson.*;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;


public class Interrogate 
{
	static GitHubClient client = new GitHubClient();
	public static String DESTINATION = "";
	public static String CURRENT_DATE = "";
	
	public static void main(String[] args) throws IOException 
	{
		client.setCredentials("", "a7316ed0fdfc9d8bdfe96c2642953a45cae7e49a");
		Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");
        CURRENT_DATE = sdf.format(date);
        DESTINATION = new File("").getAbsolutePath() + "/";

		
		String user = "";	//set default user	
		buildDatabase(user);
	}

	public static List<String> getFollowings(String user) throws IOException
	{
		UserService service = new UserService(client);
		List<User> followingList = service.getFollowing(user);
		List<String> stringList = new ArrayList<String>();
		for(int i=0;i<followingList.size();i++)
		{
			stringList.add(followingList.get(i).getLogin());
		}
		return stringList;
	}

	public static int getRepos(String user) throws IOException
	{
		UserService service = new UserService(client);
		User u = service.getUser(user);
		return u.getPublicRepos()+u.getTotalPrivateRepos();
	}

	public static void buildDatabase(String user) throws IOException
	{
		int limit = 50;
		int index = 0;
		List<LinkIndex> linkIndex = new ArrayList<LinkIndex>();
		Queue<String> q = new LinkedList<>();
		Map<String, Integer> userIndex = new HashMap<>();
		ToJSON tj = new ToJSON();
		q.add(user);
		userIndex.put(user, index);
		tj.users.add(new UserData(user, getRepos(user)));

		index++;
		while(!q.isEmpty()&&index<limit)
		{
			user = q.remove();
			int x = getRepos(user);
			List<String> followings = getFollowings(user);
			for(String follow: followings)
			{
				if(!userIndex.containsKey(follow))
				{
					q.add(follow);
					userIndex.put(follow, index);
					x = getRepos(follow);
					tj.users.add(new UserData(follow, x));
					index++;
					if(index==limit)
					{
						linkIndex.add(new LinkIndex(userIndex.get(user), userIndex.get(follow)));
						break;
					}				
				}
				linkIndex.add(new LinkIndex(userIndex.get(user), userIndex.get(follow)));
			}
		}
//		for (String key : userIndex.keySet()) {
//			System.out.print(key+": ");
//			System.out.println(userIndex.get(key));
//		}
//		System.out.println();
//		for(LinkIndex x : linkIndex)
//		{
//			System.out.println(x.source+" : "+x.to);
//		}
		
		tj.index = linkIndex;
		System.out.println(tj.users.size());
		System.out.println(tj.index.size());
		writeJSON(tj);
	}
	
	static void writeJSON(ToJSON tj) throws JsonProcessingException
	{
		 // write objects to JSON file
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String usersJSON = mapper.writeValueAsString(tj);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DESTINATION + "/" + CURRENT_DATE + "_" +
                "users_follows" + ".json"))) {
            bw.write(usersJSON);
        } catch (Exception e) {
            System.out.println("Issue with createUsersFollowsJSON()");
        }
	}
}
