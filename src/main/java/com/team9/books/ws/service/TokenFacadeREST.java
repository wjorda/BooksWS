package com.team9.books.ws.service;

import com.team9.books.ws.Token;
import com.team9.books.ws.User;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Date;
import java.util.List;

@Stateless
@Named("tokens")
@Path("login")
public class TokenFacadeREST extends AbstractFacade<Token> {

    @PersistenceContext(unitName = "com.team9_Books-WS_war_1.0PU")
    private EntityManager em;

    @EJB private UserFacadeREST users;

    public TokenFacadeREST() {
        super(Token.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Token login(@HeaderParam("username") String username,
                       @HeaderParam("password") String pwd) {
        User user = users.findByUsername(username);
        if(user == null) throw new NotFoundException("User not found");

        if(BCrypt.checkpw(pwd, new String(user.getPassHash()))) {
            Token t = new Token();
            t.setUser(user);
            t.setCreated(new Date());
            super.create(t);
            return t;
        } else throw new NotAuthorizedException("Invalid password");
    }

    @GET
    @Override
    @Produces(MediaType.APPLICATION_JSON)
    public List<Token> findAll() {
        return super.findAll();
    }

    @DELETE
    public String logout(@HeaderParam("tokenid") Integer tokenid) {
        Token t = getTokenOr401(this, tokenid);
        User user = t.getUser();
        return logout(user);
    }

    public String logout(User user) {
        //language=MySQL
        String query = "DELETE FROM Token WHERE user = :userid";

        // TODO: purge expired tokens

        this.em.createQuery(query).setParameter("userid", user).executeUpdate();

        return "Logged out user " + user.getUsername();
    }

    public static Token getTokenOr401(TokenFacadeREST tokens, int tokenid) {
        Token token = tokens.find(tokenid);
        if (token == null)
            throw new NotAuthorizedException("Token is missing, invalid, or expired."); // Return 401
        token.setCreated(new Date());
        tokens.edit(token);
        return token;
    }

    public UserFacadeREST getUsers()
    {
            return users;
    }

    public void setUsers(UserFacadeREST users)
    {
            this.users = users;
    }
}
