package com.iorga.iraj.util;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.mysema.query.codegen.EntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 479933744;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final BooleanPath active = createBoolean("active");

    public final DateTimePath<java.util.Date> creationDate = createDateTime("creationDate", java.util.Date.class);

    public final StringPath creationLogin = createString("creationLogin");

    public final StringPath firstName = createString("firstName");

    public final StringPath lastName = createString("lastName");

    public final StringPath login = createString("login");

    public final DateTimePath<java.util.Date> modificationDate = createDateTime("modificationDate", java.util.Date.class);

    public final StringPath modificationLogin = createString("modificationLogin");

    public final StringPath password = createString("password");

    public final QProfile profile;

    public final NumberPath<Integer> userId = createNumber("userId", Integer.class);

    public final DateTimePath<java.util.Date> version = createDateTime("version", java.util.Date.class);

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    @SuppressWarnings("all")
    public QUser(Path<? extends User> path) {
        this((Class)path.getType(), path.getMetadata(), path.getMetadata().isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QUser(PathMetadata<?> metadata) {
        this(metadata, metadata.isRoot() ? INITS : PathInits.DEFAULT);
    }

    public QUser(PathMetadata<?> metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata<?> metadata, PathInits inits) {
        super(type, metadata, inits);
        this.profile = inits.isInitialized("profile") ? new QProfile(forProperty("profile")) : null;
    }

}

