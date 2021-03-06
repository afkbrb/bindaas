package edu.emory.cci.bindaas.trusted_app.impl;

import java.security.MessageDigest;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.emory.cci.bindaas.core.apikey.api.APIKey;
import edu.emory.cci.bindaas.core.apikey.api.APIKeyManagerException;
import edu.emory.cci.bindaas.core.apikey.api.IAPIKeyManager;
import edu.emory.cci.bindaas.core.config.BindaasConfiguration;
import edu.emory.cci.bindaas.core.jwt.IJWTManager;
import edu.emory.cci.bindaas.core.jwt.JWTManagerException;
import edu.emory.cci.bindaas.core.model.hibernate.HistoryLog.ActivityType;
import edu.emory.cci.bindaas.core.model.hibernate.UserRequest;
import edu.emory.cci.bindaas.core.util.DynamicObject;
import edu.emory.cci.bindaas.framework.provider.exception.AbstractHttpCodeException;
import edu.emory.cci.bindaas.framework.util.GSONUtil;
import edu.emory.cci.bindaas.security.api.BindaasUser;

import edu.emory.cci.bindaas.trusted_app.TrustedApplicationRegistry;
import edu.emory.cci.bindaas.trusted_app.TrustedApplicationRegistry.TrustedApplicationEntry;
import edu.emory.cci.bindaas.trusted_app.api.ITrustedApplicationManager;
import edu.emory.cci.bindaas.trusted_app.bundle.Activator;
import edu.emory.cci.bindaas.trusted_app.exception.APIKeyDoesNotExistException;
import edu.emory.cci.bindaas.trusted_app.exception.DuplicateAPIKeyException;
import edu.emory.cci.bindaas.trusted_app.exception.DuplicateJWTException;
import edu.emory.cci.bindaas.trusted_app.exception.JWTDoesNotExistException;
import edu.emory.cci.bindaas.trusted_app.exception.NotAuthorizedException;
import edu.emory.cci.bindaas.trusted_app.constants.TrustedAppConstants;

public class TrustedApplicationManagerImpl implements
		ITrustedApplicationManager {

	private TrustedApplicationRegistry defaultTrustedApplications;
	private DynamicObject<TrustedApplicationRegistry> trustedApplicationRegistry;
	private String bindaasConfigurationProtocol;
	private final static long ROUNDOFF_FACTOR = 100000;
	private IAPIKeyManager apiKeyManager;
	private IJWTManager JWTManager;
	private Log log = LogFactory.getLog(getClass());

	public static final String protocolDoesNotMatchError = "Authentication protocol in request does not match with server's configuration";
	public static final String cannotIssueTokenError = "Can not issue token with protocol [JWT]";
	public static final String cannotAuthorizeUserError = "Can not authorize user with protocol [JWT]";

	public IAPIKeyManager getApiKeyManager() {
		return apiKeyManager;
	}

	public void setApiKeyManager(IAPIKeyManager apiKeyManager) {
		this.apiKeyManager = apiKeyManager;
	}

	public IJWTManager getJWTManager() {
		return JWTManager;
	}

	public void setJWTManager(IJWTManager JWTManager) {
		this.JWTManager = JWTManager;
	}

	public TrustedApplicationRegistry getDefaultTrustedApplications() {
		return defaultTrustedApplications;
	}

	public void setDefaultTrustedApplications(
			TrustedApplicationRegistry defaultTrustedApplications) {
		this.defaultTrustedApplications = defaultTrustedApplications;
	}

	public void init() throws Exception {

		@SuppressWarnings("unchecked")
		DynamicObject<BindaasConfiguration> bindaasConfiguration = Activator.getService(DynamicObject.class , "(name=bindaas)");
		bindaasConfigurationProtocol = bindaasConfiguration.getObject().getAuthenticationProtocol();

		final BundleContext context = Activator.getContext();
		trustedApplicationRegistry = new DynamicObject<TrustedApplicationRegistry>(
				"trusted-applications", defaultTrustedApplications, context);

		String filterExpression = "(&(objectclass=edu.emory.cci.bindaas.core.util.DynamicObject)(name=bindaas))";
		Filter filter = FrameworkUtil.createFilter(filterExpression);

		final ITrustedApplicationManager ref = this;
		@SuppressWarnings({ "rawtypes", "unchecked" })
		ServiceTracker<?, ?> serviceTracker = new ServiceTracker(context,
				filter, new ServiceTrackerCustomizer() {

					@Override
					public Object addingService(ServiceReference srf) {
						@SuppressWarnings("unchecked")
						DynamicObject<BindaasConfiguration> dynamicConfiguration = (DynamicObject<BindaasConfiguration>) context
								.getService(srf);
						Dictionary<String, Object> testProps = new Hashtable<String, Object>();
						testProps
								.put("edu.emory.cci.bindaas.commons.cxf.service.name",
										"Trusted Application Manager");

						if (dynamicConfiguration != null
								&& dynamicConfiguration.getObject() != null) {
							BindaasConfiguration configuration = dynamicConfiguration
									.getObject();
							String publishUrl = "http://"
									+ configuration.getHost() + ":"
									+ configuration.getPort();
							testProps
									.put("edu.emory.cci.bindaas.commons.cxf.service.address",
											publishUrl + "/trustedApplication");
							context.registerService(
									ITrustedApplicationManager.class, ref,
									testProps);
						}

						return null;
					}

					@Override
					public void modifiedService(ServiceReference arg0,
							Object arg1) {
						// do nothing

					}

					@Override
					public void removedService(ServiceReference arg0,
							Object arg1) {
						// do nothing

					}

				});

		serviceTracker.open();

	}

	public DynamicObject<TrustedApplicationRegistry> getTrustedApplicationRegistry() {
		return trustedApplicationRegistry;
	}

	public void setTrustedApplicationRegistry(
			DynamicObject<TrustedApplicationRegistry> trustedApplicationRegistry) {
		this.trustedApplicationRegistry = trustedApplicationRegistry;
	}

	private APIKey generateApiKey(BindaasUser principal, Integer lifespan,
			String clientId) throws Exception {

		APIKey apiKey = apiKeyManager.createShortLivedAPIKey(principal,
				lifespan, clientId);
		return apiKey;
	}

	private Response exceptionToResponse(AbstractHttpCodeException exception,
			String applicationID, String applicationName, String username) {
		JsonObject retVal = new JsonObject();
		retVal.add("applicationID", new JsonPrimitive(applicationID));
		retVal.add("applicationName", new JsonPrimitive(applicationName));
		retVal.add("username", new JsonPrimitive(username));
		retVal.add("error", exception.toJson());
		return Response.status(exception.getHttpStatusCode())
				.entity(retVal.toString()).type("application/json").build();
	}

	private Response exceptionToResponse(String message){
		JsonObject retVal = new JsonObject();
		retVal.add("error",new JsonPrimitive(message));
		return Response.status(500).entity(retVal.toString()).
				type("application/json").build();
	}

	@Override
	@GET
	@Path("/issueShortLivedAuthenticationToken")
	public Response issueShortLivedAuthenticationToken(@HeaderParam("_protocol") String protocol,
			@HeaderParam("_username") String username,
			@HeaderParam("_applicationID") String applicationID,
			@HeaderParam("_salt") String salt,
			@HeaderParam("_digest") String digest,
			@QueryParam("lifetime") Integer lifetime) {

		if(bindaasConfigurationProtocol.equalsIgnoreCase(protocol) && protocol.equals("api_key")) {
			try {

				TrustedApplicationEntry trustedAppEntry = authenticateTrustedApplication(
						applicationID, salt, digest, username);

				BindaasUser bindaasUser = new BindaasUser(username);
				JsonObject retVal = new JsonObject();

				int lifespan;
				String logMsg;

				if ((lifetime == null) || lifetime <=0){
					lifespan = TrustedAppConstants.DEFAULT_LIFESPAN_OF_KEY_IN_SECONDS;
					logMsg = "The user did not request a positive lifetime for the key. Therefore, the time limit " +
							"will be set to " + lifespan + " seconds.";
					log.info(logMsg);
					retVal.add("comments", new JsonPrimitive(logMsg));

				} else if (lifetime > TrustedAppConstants.MAXIMUM_LIFE_TIME_FOR_SHORT_LIVED_API_KEYS) {
					lifespan = TrustedAppConstants.MAXIMUM_LIFE_TIME_FOR_SHORT_LIVED_API_KEYS;
					logMsg = "The user requested lifetime [" + lifetime + "] exceeds the time limit " +
							"set by the system for a short-lived API Key: " +
							TrustedAppConstants.MAXIMUM_LIFE_TIME_FOR_SHORT_LIVED_API_KEYS + " seconds. Therefore, " +
							"the time limit is set to " + lifespan + " seconds.";
					log.info(logMsg);
					retVal.add("comments", new JsonPrimitive(logMsg));

				} else {
					lifespan = lifetime;
				}

				String applicationName = trustedAppEntry.getName();

				APIKey sessionKey = generateApiKey(bindaasUser, lifespan,
						applicationName);
				retVal.add("api_key", new JsonPrimitive(sessionKey.getValue()));
				retVal.add("username", new JsonPrimitive(username));
				retVal.add("applicationID", new JsonPrimitive(applicationID));
				retVal.add("expires", new JsonPrimitive(sessionKey.getExpires()
						.toString()));
				retVal.add("applicationName", new JsonPrimitive(applicationName));
				return Response.ok().entity(retVal.toString())
						.type("application/json").build();

			} catch (NotAuthorizedException e) {
				log.error(e.getErrorDescription());
				return exceptionToResponse(e, applicationID, "not-resolved",
						username);

			} catch (APIKeyManagerException apiKeyManagerException) {
				switch (apiKeyManagerException.getReason()) {
					case KEY_DOES_NOT_EXIST:
						log.error(apiKeyManagerException.getMessage());
						return exceptionToResponse(new APIKeyDoesNotExistException(
								username), applicationID, "not-resolved", username);
					default:
						log.error(apiKeyManagerException.getMessage());
						return exceptionToResponse(apiKeyManagerException.getMessage());
				}
			} catch (Exception e) {
				log.error(e.getMessage());
				return exceptionToResponse(e.getMessage());
			}
		}
		else if (bindaasConfigurationProtocol.equalsIgnoreCase(protocol) && protocol.equals("jwt")) {
			log.error(cannotIssueTokenError);
			return exceptionToResponse(cannotIssueTokenError);

		}
		else {
			log.error(protocolDoesNotMatchError);
			return exceptionToResponse(protocolDoesNotMatchError);
		}

	}

	public static String calculateDigestValue(String applicationID,
			String applicationKey, String salt, String username)
			throws Exception {
		long roundoff = System.currentTimeMillis() / ROUNDOFF_FACTOR;
		String prenonce = roundoff + "|" + applicationKey;
		byte[] nonceBytes = MessageDigest.getInstance("SHA-1").digest(
				prenonce.getBytes("UTF-8"));
		String nonce = DatatypeConverter.printBase64Binary(nonceBytes);

		String predigest = String.format("%s|%s|%s|%s", username, nonce,
				applicationID, salt);
		String digest = DatatypeConverter.printBase64Binary(MessageDigest
				.getInstance("SHA-1").digest(predigest.getBytes("UTF-8")));

		return digest;
	}

	private TrustedApplicationEntry authenticateTrustedApplication(
			String applicationID, String salt, String digest, String username)
			throws NotAuthorizedException {
		try {
			TrustedApplicationRegistry registry = trustedApplicationRegistry
					.getObject();
			TrustedApplicationEntry entry = registry.lookup(applicationID);
			if (entry != null) {
				String applicationKey = entry.getApplicationKey();
				String caclulatedDigest = calculateDigestValue(applicationID,
						applicationKey, salt, username);
				if (caclulatedDigest.equals(digest)) {
					return entry;
				} else
					throw new NotAuthorizedException(
							"Failed to authenticate Trusted Application applicationID=["
									+ applicationID + "] . Wrong digest value");

			} else {
				throw new NotAuthorizedException(
						"No TrustedApplication found for applicationID=["
								+ applicationID + "]");
			}

		} catch (NotAuthorizedException e) {
			throw e;
		} catch (Exception e) {
			throw new NotAuthorizedException(e.getMessage());
		}

	}

	@Override
	@GET
	@Path("/authorizeUser")
	public Response authorizeNewUser(@HeaderParam("_protocol") String protocol,
			@HeaderParam("_username") String username,
			@HeaderParam("_applicationID") String applicationID,
			@HeaderParam("_salt") String salt,
			@HeaderParam("_digest") String digest,
			@QueryParam("expires") Long epochTime,
			@QueryParam("comments") String comments) {

		if(bindaasConfigurationProtocol.equalsIgnoreCase(protocol) && protocol.equals("api_key")){
			try {

				TrustedApplicationEntry trustedAppEntry = authenticateTrustedApplication(
						applicationID, salt, digest, username);
				Date dateExpires = new Date(epochTime);
				if (comments == null) {
					comments = "API Key Generated via Trusted Application API";
				}

				APIKey apiKey = apiKeyManager.generateAPIKey(new BindaasUser(
								username), dateExpires, trustedAppEntry.getName(),
						comments, ActivityType.APPROVE, true);

				JsonObject retVal = new JsonObject();
				retVal.add("api_key", new JsonPrimitive(apiKey.getValue()));
				retVal.add("username", new JsonPrimitive(username));
				retVal.add("applicationID", new JsonPrimitive(applicationID));
				retVal.add("expires", new JsonPrimitive(apiKey.getExpires()
						.toString()));
				retVal.add("applicationName",
						new JsonPrimitive(trustedAppEntry.getName()));
				return Response.ok().entity(retVal.toString())
						.type("application/json").build();

			} catch (NotAuthorizedException e) {
				log.error(e.getErrorDescription());
				return exceptionToResponse(e, applicationID, "not-resolved",
						username);

			} catch (APIKeyManagerException apiKeyManagerException) {
				switch (apiKeyManagerException.getReason()) {
					case KEY_ALREADY_EXIST:
						log.error(apiKeyManagerException.getMessage());
						return exceptionToResponse(new DuplicateAPIKeyException(
								username), applicationID, "not-resolved", username);
					default:
						log.error(apiKeyManagerException.getMessage());
						return exceptionToResponse(apiKeyManagerException.getMessage());
				}
			} catch (Exception e) {
				log.error(e.getMessage());
				return exceptionToResponse(e.getMessage());

			}

		}
		else if (bindaasConfigurationProtocol.equalsIgnoreCase(protocol) && protocol.equals("jwt")) {

			log.error(cannotAuthorizeUserError);
			return exceptionToResponse(cannotAuthorizeUserError);
		}
		else {
			log.error(protocolDoesNotMatchError);
			return exceptionToResponse(protocolDoesNotMatchError);
		}

	}

	@Override
	@DELETE
	@Path("/revokeUser")
	public Response revokeAccess(@HeaderParam("_protocol") String protocol,
			@HeaderParam("_username") String username,
			@HeaderParam("_applicationID") String applicationID,
			@HeaderParam("_salt") String salt,
			@HeaderParam("_digest") String digest,
			@QueryParam("comments") String comments) {

		if(bindaasConfigurationProtocol.equalsIgnoreCase(protocol) && protocol.equals("api_key")){
			try {
				TrustedApplicationEntry trustedAppEntry = authenticateTrustedApplication(
						applicationID, salt, digest, username);

				if (comments == null) {
					comments = "API Key revoked via Trusted Application API";
				}

				Integer count = apiKeyManager.revokeAPIKey(
						new BindaasUser(username), trustedAppEntry.getName(),
						comments, ActivityType.REVOKE);

				JsonObject retVal = new JsonObject();
				retVal.add("username", new JsonPrimitive(username));
				retVal.add("keys_deleted", new JsonPrimitive(count));
				retVal.add("applicationID", new JsonPrimitive(applicationID));

				retVal.add("applicationName",
						new JsonPrimitive(trustedAppEntry.getName()));
				return Response.ok().entity(retVal.toString())
						.type("application/json").build();

			} catch (NotAuthorizedException e) {
				log.error(e.getErrorDescription());
				return exceptionToResponse(e, applicationID, "not-resolved",
						username);

			} catch (Exception e) {
				log.error(e.getMessage());
				return exceptionToResponse(e.getMessage());
			}
		}
		else if (bindaasConfigurationProtocol.equalsIgnoreCase(protocol) && protocol.equals("jwt")) {
			try {
				TrustedApplicationEntry trustedAppEntry = authenticateTrustedApplication(
						applicationID, salt, digest, username);

				if (comments == null) {
					comments = "JWT revoked via Trusted Application API";
				}

				Integer count = JWTManager.revokeJWT(new BindaasUser(username),
						trustedAppEntry.getName(), comments, ActivityType.REVOKE);

				JsonObject retVal = new JsonObject();
				retVal.add("username", new JsonPrimitive(username));
				retVal.add("tokens_deleted", new JsonPrimitive(count));
				retVal.add("applicationID", new JsonPrimitive(applicationID));

				retVal.add("applicationName",
						new JsonPrimitive(trustedAppEntry.getName()));
				return Response.ok().entity(retVal.toString())
						.type("application/json").build();

			} catch (NotAuthorizedException e) {
				log.error(e.getErrorDescription());
				return exceptionToResponse(e, applicationID, "not-resolved",
						username);

			} catch (Exception e) {
				log.error(e.getMessage());
				return exceptionToResponse(e.getMessage());
			}
		}
		else {
			log.error(protocolDoesNotMatchError);
			return exceptionToResponse(protocolDoesNotMatchError);
		}

	}

	@Override
	@GET
	@Path("/listAuthenticationTokens")
	public Response listAuthenticationTokens(@HeaderParam("_protocol") String protocol,
			@HeaderParam("_username") String username,
			@HeaderParam("_applicationID") String applicationID,
			@HeaderParam("_salt") String salt,
			@HeaderParam("_digest") String digest) {

		if(bindaasConfigurationProtocol.equalsIgnoreCase(protocol) && protocol.equals("api_key")) {
			try {

				TrustedApplicationEntry trustedAppEntry = authenticateTrustedApplication(
						applicationID, salt, digest, username);

				List<UserRequest> listOfApiKeys = apiKeyManager.listAPIKeys();
				JsonArray array = GSONUtil.getGSONInstance()
						.toJsonTree(listOfApiKeys).getAsJsonArray();
				JsonObject retVal = new JsonObject();

				retVal.add("authenticationTokens", array);
				retVal.add("applicationID", new JsonPrimitive(applicationID));
				retVal.add("applicationName",
						new JsonPrimitive(trustedAppEntry.getName()));
				return Response.ok().entity(retVal.toString())
						.type("application/json").build();

			} catch (NotAuthorizedException e) {
				log.error(e.getErrorDescription());
				return exceptionToResponse(e, applicationID, "not-resolved",
						username);

			} catch (Exception e) {
				log.error(e.getMessage());
				return exceptionToResponse(e.getMessage());
			}
		}
		else if (bindaasConfigurationProtocol.equalsIgnoreCase(protocol) && protocol.equals("jwt")) {
			try {

				TrustedApplicationEntry trustedAppEntry = authenticateTrustedApplication(
						applicationID, salt, digest, username);

				List<UserRequest> listOfJWT = JWTManager.listJWT();
				JsonArray array = GSONUtil.getGSONInstance()
						.toJsonTree(listOfJWT).getAsJsonArray();
				JsonObject retVal = new JsonObject();

				retVal.add("authenticationTokens", array);
				retVal.add("applicationID", new JsonPrimitive(applicationID));
				retVal.add("applicationName",
						new JsonPrimitive(trustedAppEntry.getName()));
				return Response.ok().entity(retVal.toString())
						.type("application/json").build();

			} catch (NotAuthorizedException e) {
				log.error(e.getErrorDescription());
				return exceptionToResponse(e, applicationID, "not-resolved",
						username);

			} catch (Exception e) {
				log.error(e.getMessage());
				return exceptionToResponse(e.getMessage());
			}
		}
		else {
			log.error(protocolDoesNotMatchError);
			return exceptionToResponse(protocolDoesNotMatchError);
		}

	}

}