def propertyKey = 'detect.docker.thing.one.more.thing'
def pieces = propertyKey.split('\\.')
def javaName = pieces[1] + pieces[2..-1].collect {
    it[0].toUpperCase() + it[1..-1]
}.join('')
println propertyKey
println javaName